package com.ultimatebank.core.snapshots;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ultimatebank.UltimateBankConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

/**
 * Remembers what's in the bank and each POH costume room storage, per account, so features can work before (or
 * without) the player opening them again. Keeps the last few snapshots of each location for the visit history,
 * one per visit: while a location stays open its latest snapshot is updated in place, so the previous snapshot shows
 * everything taken or deposited during the visit.
 * Always on: registered on the EventBus by the plugin itself. All methods must be called on the client thread.
 */
@Slf4j
@Singleton
public class StorageSnapshots
{
	private static final String KEY_PREFIX = "snapshots.";
	/** The costume room interface fills in its items a couple of ticks after it opens */
	private static final int POH_SCAN_DELAY_TICKS = 2;
	private static final int POH_SCAN_ATTEMPTS = 6;
	/** Varbit storages are read this many ticks after logging in */
	private static final int LOGIN_SCAN_DELAY_TICKS = 2;
	/** [clientscript,script861]: the key ring interface showing a key, with (current, previous, next, total) */
	private static final int KEY_RING_SCRIPT = 861;
	private final Client client;
	private final ConfigManager configManager;
	private final UltimateBankConfig config;
	private final Gson gson;
	private final PohStorageScanner pohScanner;
	private final PotionStorageReader potionStorage;

	/** Oldest first */
	private final Map<SnapshotLocation, List<Snapshot>> histories = new EnumMap<>(SnapshotLocation.class);
	private final List<Consumer<SnapshotLocation>> listeners = new CopyOnWriteArrayList<>();
	/** Locations whose latest snapshot was appended during the current visit, so further changes replace it */
	private final Set<SnapshotLocation> visitEntries = EnumSet.noneOf(SnapshotLocation.class);
	private String loadedProfile;
	private boolean bankScanPending;
	/** The bank has been read since it was opened */
	private boolean bankVisit;
	/** The POH storage currently open, once read */
	private SnapshotLocation pohVisit;
	private int pohScanDelay = -1;
	private int pohScanAttemptsLeft;
	/** Container and varbit storages whose interface is open */
	private final Set<SnapshotLocation> openStorages = EnumSet.noneOf(SnapshotLocation.class);
	/** Container and varbit storages to read next tick */
	private final Set<SnapshotLocation> storageScans = EnumSet.noneOf(SnapshotLocation.class);
	/** Container and varbit storages read since their interface last opened */
	private final Set<SnapshotLocation> readThisOpen = EnumSet.noneOf(SnapshotLocation.class);
	/** Ticks until varbit storages are read after logging in (their varbits arrive with the login) */
	private int loginScanDelay = -1;
	/** The menagerie's pet varps changed: read the pet house next tick, open or not */
	private boolean petHouseScanPending;
	/** Item IDs in the inventory now and before its last change, to name a key put on the key ring */
	private Set<Integer> inventoryIds = Set.of();
	private Set<Integer> previousInventoryIds = Set.of();
	/** Keys seen on the steel key ring this login. Kept across its openings: every Next click reopens it. */
	private KeyRingKeys keyRing = new KeyRingKeys();
	/** The key ring has been open since the last tick it was seen closed */
	private boolean keyRingVisit;
	private int keyRingSeenLogged = -1;
	/** The potion store may have changed: read it next tick while the bank is open */
	private boolean potionScanPending;

	@Inject
	StorageSnapshots(Client client, ConfigManager configManager, UltimateBankConfig config, Gson gson,
		PohStorageScanner pohScanner, PotionStorageReader potionStorage)
	{
		this.client = client;
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
		this.pohScanner = pohScanner;
		this.potionStorage = potionStorage;
	}

	/**
	 * @return the most recent snapshot of a location, or null if it's never been seen on this account
	 */
	public Snapshot latest(SnapshotLocation location)
	{
		List<Snapshot> history = histories.get(location);
		return history == null || history.isEmpty() ? null : history.get(history.size() - 1);
	}

	/**
	 * @return the snapshot before the latest one with different contents, or null
	 */
	public Snapshot previous(SnapshotLocation location)
	{
		List<Snapshot> history = histories.get(location);
		return history == null || history.size() < 2 ? null : history.get(history.size() - 2);
	}

	/**
	 * @return every kept snapshot of a location, oldest first (one per visit); empty if it's never been seen
	 */
	public List<Snapshot> history(SnapshotLocation location)
	{
		List<Snapshot> history = histories.get(location);
		return history == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(history));
	}

	/**
	 * Called with the location whenever a snapshot is recorded, or with null when the account's snapshots are
	 * (re)loaded. Remove on shutdown.
	 */
	public void addListener(Consumer<SnapshotLocation> listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Consumer<SnapshotLocation> listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Loads the current account's snapshots, if a different account's (or none) are loaded.
	 */
	public void load()
	{
		String profile = configManager.getRSProfileKey();
		if (profile == null || profile.equals(loadedProfile))
		{
			return;
		}
		loadedProfile = profile;
		histories.clear();
		endVisits();
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			String json = configManager.getRSProfileConfiguration(UltimateBankConfig.GROUP, key(location));
			if (json == null || json.isEmpty())
			{
				continue;
			}
			try
			{
				List<Snapshot> history = gson.fromJson(json, new TypeToken<List<Snapshot>>()
				{
				}.getType());
				if (history != null && !history.isEmpty())
				{
					histories.put(location, new ArrayList<>(history));
				}
			}
			catch (RuntimeException e)
			{
				log.warn("Ignoring unreadable {} snapshot", location, e);
			}
		}
		log.debug("Loaded snapshots for {} locations", histories.size());
		notifyListeners(null);
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		load();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			load();
			loginScanDelay = LOGIN_SCAN_DELAY_TICKS;
		}
		else
		{
			endVisits();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.INV)
		{
			previousInventoryIds = inventoryIds;
			inventoryIds = new HashSet<>();
			for (Item item : event.getItemContainer().getItems())
			{
				if (item.getId() > 0)
				{
					inventoryIds.add(item.getId());
				}
			}
		}

		if (event.getContainerId() == InventoryID.BANK)
		{
			// Read it next tick, once the bank has finished updating
			bankScanPending = true;
		}
		else if (ContainerStorage.forInventory(event.getContainerId()) != null)
		{
			storageScans.add(ContainerStorage.forInventory(event.getContainerId()).location);
		}
		else if (event.getContainerId() == InventoryID.INV && client.getWidget(InterfaceID.PohCostumes.UNIVERSE) != null)
		{
			// Taking something out of (or putting it into) POH storage changes the inventory; the storage interface
			// fires nothing of its own, so read it again next tick
			pohScanDelay = 1;
			pohScanAttemptsLeft = POH_SCAN_ATTEMPTS;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.POH_COSTUMES)
		{
			pohScanDelay = POH_SCAN_DELAY_TICKS;
			pohScanAttemptsLeft = POH_SCAN_ATTEMPTS;
		}
		if (event.getGroupId() == InterfaceID.FAVOUR_KEYRING && !keyRingVisit)
		{
			keyRingVisit = true;
			readThisOpen.remove(SnapshotLocation.STEEL_KEY_RING);
		}

		SnapshotLocation storage = interfaceStorage(event.getGroupId());
		if (storage != null)
		{
			openStorages.add(storage);
			readThisOpen.remove(storage);
			// Its contents may already be loaded and not change, so read it anyway
			storageScans.add(storage);
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		SnapshotLocation storage = interfaceStorage(event.getGroupId());
		if (storage != null)
		{
			openStorages.remove(storage);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		// Depositing at a varbit storage changes its varbits: read whichever is open again next tick
		for (VarbitStorage storage : VarbitStorage.values())
		{
			if (openStorages.contains(storage.location))
			{
				storageScans.add(storage.location);
			}
		}
		if (bankVisit)
		{
			// Potion store doses and vials are varps
			potionScanPending = true;
		}
		int varp = event.getVarpId();
		if (varp == VarPlayerID.PRAYER20 || varp == VarPlayerID.MENAGERIE_CONTENTS2
			|| varp == VarPlayerID.MENAGERIE_CONTENTS3)
		{
			petHouseScanPending = true;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		// Using a key on the key ring doesn't open it; only this message says what changed. While it's open, its
		// interface already shows the change.
		if (event.getType() != ChatMessageType.GAMEMESSAGE || keyRingVisit)
		{
			return;
		}
		String added = KeyRingKeys.addedKey(event.getMessage());
		String removed = KeyRingKeys.removedKey(event.getMessage());
		Snapshot latest = latest(SnapshotLocation.STEEL_KEY_RING);
		// Without a full read of the ring first, one key says nothing about the rest
		if (added == null && removed == null || latest == null)
		{
			return;
		}

		Set<Integer> keys = new HashSet<>(latest.getQuantities().keySet());
		Set<Integer> candidates = new HashSet<>(inventoryIds);
		candidates.addAll(previousInventoryIds);
		candidates.addAll(keys);
		int key = itemNamed(added != null ? added : removed, candidates);
		if (key <= 0)
		{
			log.debug("Key ring: couldn't find the item for '{}'", event.getMessage());
			return;
		}
		if (added != null)
		{
			keys.add(key);
		}
		else
		{
			keys.remove(key);
		}
		Map<Integer, Integer> contents = new HashMap<>();
		keys.forEach(k -> contents.put(k, 1));
		if (!contents.equals(latest.getQuantities()))
		{
			log.debug("Key ring: {} {}", added != null ? "added" : "removed", key);
			record(SnapshotLocation.STEEL_KEY_RING, contents);
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() != KEY_RING_SCRIPT || event.getScriptEvent() == null)
		{
			return;
		}
		Object[] args = event.getScriptEvent().getArguments();
		log.debug("Key ring script args: {}", Arrays.toString(args));
		// args[0] is the script; then the current, previous and next keys, and the total
		if (args != null && args.length > 4 && args[4] instanceof Integer)
		{
			keyRing.setTotal((Integer) args[4]);
		}
		for (int i = 1; args != null && i < Math.min(args.length, 4); i++)
		{
			if (args[i] instanceof Integer && isKey((Integer) args[i]))
			{
				if (i == 1)
				{
					keyRing.show((Integer) args[i]);
				}
				else
				{
					keyRing.see((Integer) args[i]);
				}
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (bankScanPending)
		{
			bankScanPending = false;
			scanBank();
		}
		if (potionScanPending)
		{
			potionScanPending = false;
			if (bankVisit)
			{
				scanPotionStorage();
			}
		}
		if (keyRingVisit)
		{
			if (client.getWidget(InterfaceID.FavourKeyring.KEYTOTAL) == null)
			{
				keyRingVisit = false;
				visitEntries.remove(SnapshotLocation.STEEL_KEY_RING);
			}
			else
			{
				scanKeyRing();
			}
		}

		if (pohScanDelay > 0)
		{
			pohScanDelay--;
		}
		else if (pohScanDelay == 0)
		{
			scanPoh();
		}

		if (petHouseScanPending)
		{
			petHouseScanPending = false;
			scanPetHouseVarps();
		}
		for (SnapshotLocation storage : storageScans)
		{
			scanStorage(storage);
		}
		storageScans.clear();
		for (SnapshotLocation storage : SnapshotLocation.values())
		{
			if (isInterfaceStorage(storage) && !openStorages.contains(storage))
			{
				visitEntries.remove(storage);
			}
		}

		if (loginScanDelay > 0)
		{
			loginScanDelay--;
		}
		else if (loginScanDelay == 0)
		{
			loginScanDelay = -1;
			scanVarbitStoragesAtLogin();
		}

		if (bankVisit && client.getWidget(InterfaceID.Bankmain.ITEMS) == null)
		{
			bankVisit = false;
			visitEntries.remove(SnapshotLocation.BANK);
			visitEntries.remove(SnapshotLocation.POTION_STORAGE);
		}
		if (pohVisit != null && client.getWidget(InterfaceID.PohCostumes.UNIVERSE) == null)
		{
			visitEntries.remove(pohVisit);
			pohVisit = null;
		}
	}

	private void endVisits()
	{
		visitEntries.clear();
		bankVisit = false;
		pohVisit = null;
		openStorages.clear();
		storageScans.clear();
		readThisOpen.clear();
		petHouseScanPending = false;
		keyRing = new KeyRingKeys();
		keyRingVisit = false;
		potionScanPending = false;
	}

	private void scanBank()
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		// Only trust the container while the bank is open (or it changed just as it closed)
		if (bank == null || !bankVisit && client.getWidget(InterfaceID.Bankmain.ITEMS) == null)
		{
			return;
		}
		if (!bankVisit)
		{
			// Opened: read the potion store too
			readThisOpen.remove(SnapshotLocation.POTION_STORAGE);
			potionScanPending = true;
		}
		bankVisit = true;

		Map<Integer, Integer> quantities = new HashMap<>();
		for (Item item : bank.getItems())
		{
			// Placeholders have quantity 0
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		record(SnapshotLocation.BANK, quantities);
	}

	/**
	 * Reads an open container or varbit storage (or one that changed just as it closed).
	 */
	private void scanStorage(SnapshotLocation storage)
	{
		if (!openStorages.contains(storage) && !visitEntries.contains(storage))
		{
			return;
		}
		Map<Integer, Integer> quantities = readStorage(storage);
		if (quantities != null)
		{
			recordRead(storage, quantities);
		}
	}

	/**
	 * Records a storage read while its interface is open. Re-reads often find nothing new; unchanged contents are
	 * saved once per opening (that notes when it was seen), not on every re-read.
	 */
	private void recordRead(SnapshotLocation storage, Map<Integer, Integer> quantities)
	{
		Snapshot latest = latest(storage);
		if (latest != null && readThisOpen.contains(storage) && latest.getQuantities().equals(quantities))
		{
			return;
		}
		readThisOpen.add(storage);
		log.debug("Read {}: {} items", storage, quantities.size());
		record(storage, quantities);
	}

	/**
	 * The potion store's doses are only read with the bank open. Not recorded until it holds something, so players
	 * without it get no empty location.
	 */
	private void scanPotionStorage()
	{
		Map<Integer, Integer> quantities = potionStorage.read();
		if (quantities == null || quantities.isEmpty() && latest(SnapshotLocation.POTION_STORAGE) == null)
		{
			return;
		}
		recordRead(SnapshotLocation.POTION_STORAGE, quantities);
	}

	/**
	 * The key ring shows one key at a time and its total; record it once every key is known.
	 */
	private void scanKeyRing()
	{
		Widget total = client.getWidget(InterfaceID.FavourKeyring.KEYTOTAL);
		int shownTotal = total == null ? -1 : KeyRingKeys.parseTotal(total.getText());
		if (shownTotal >= 0)
		{
			keyRing.setTotal(shownTotal);
		}
		Widget shown = client.getWidget(InterfaceID.FavourKeyring.MAIN);
		if (shown != null && isKey(shown.getItemId()))
		{
			keyRing.see(shown.getItemId());
		}

		Snapshot latest = latest(SnapshotLocation.STEEL_KEY_RING);
		Map<Integer, Integer> keys = keyRing.contents(latest == null ? Set.of() : latest.getQuantities().keySet());
		if (keys == null)
		{
			if (keyRing.seenCount() != keyRingSeenLogged)
			{
				keyRingSeenLogged = keyRing.seenCount();
				log.debug("Key ring: {} of {} keys seen (shown item {})", keyRing.seenCount(), keyRing.getTotal(),
					shown == null ? null : shown.getItemId());
			}
			return;
		}
		recordRead(SnapshotLocation.STEEL_KEY_RING, keys);
	}

	private boolean isKey(int itemId)
	{
		return itemId > 0 && client.getItemDefinition(itemId).getName().toLowerCase(Locale.ROOT).contains("key");
	}

	/**
	 * Varbit storages are known without opening them. At login, record any that changed since last seen (the first
	 * time, or after playing elsewhere). An empty read is skipped: the varbits may not be in yet, and emptying one
	 * happens through its interface, which is recorded then.
	 */
	private void scanVarbitStoragesAtLogin()
	{
		for (VarbitStorage storage : VarbitStorage.values())
		{
			Map<Integer, Integer> quantities = readStorage(storage.location);
			Snapshot latest = latest(storage.location);
			if (quantities == null || quantities.isEmpty()
				|| latest != null && latest.getQuantities().equals(quantities))
			{
				continue;
			}
			log.debug("Read {} at login: {} items", storage.location, quantities.size());
			record(storage.location, quantities);
		}
	}

	/**
	 * @return a container or varbit storage's contents, or null if they can't be read now
	 */
	private Map<Integer, Integer> readStorage(SnapshotLocation location)
	{
		for (VarbitStorage storage : VarbitStorage.values())
		{
			if (storage.location == location)
			{
				return storage.read(client::getVarbitValue, this::wateringCan);
			}
		}

		ContainerStorage storage = ContainerStorage.forLocation(location);
		if (storage == null)
		{
			return null;
		}
		ItemContainer container = client.getItemContainer(storage.inventoryId);
		// The menagerie's container may not exist when it holds no pet items, but its pets are still known
		if (container == null && storage != ContainerStorage.PET_HOUSE)
		{
			return null;
		}
		Map<Integer, Integer> quantities = new HashMap<>();
		for (Item item : container == null ? new Item[0] : container.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		if (storage == ContainerStorage.PET_HOUSE)
		{
			Set<Integer> pets = menageriePets();
			if (pets != null)
			{
				pets.forEach(pet -> quantities.putIfAbsent(pet, 1));
			}
		}
		return quantities;
	}

	/**
	 * Dropping a pet in the house (or taking one out) changes the menagerie's varps without opening it: record the
	 * pets now, keeping the pet items from when it was last open. While it's open, it's read as usual.
	 */
	private void scanPetHouseVarps()
	{
		if (openStorages.contains(SnapshotLocation.PET_HOUSE))
		{
			storageScans.add(SnapshotLocation.PET_HOUSE);
			return;
		}
		Set<Integer> pets = menageriePets();
		EnumComposition petEnum = client.getEnum(MenageriePets.PETS_ENUM);
		Snapshot latest = latest(SnapshotLocation.PET_HOUSE);
		if (pets == null || petEnum == null || latest == null && pets.isEmpty())
		{
			return;
		}

		Set<Integer> allPets = new HashSet<>();
		for (int pet : petEnum.getIntVals())
		{
			allPets.add(pet);
		}
		Map<Integer, Integer> quantities = new HashMap<>();
		boolean hadPets = false;
		if (latest != null)
		{
			for (Map.Entry<Integer, Integer> entry : latest.getQuantities().entrySet())
			{
				if (allPets.contains(entry.getKey()))
				{
					hadPets = true;
				}
				else
				{
					quantities.put(entry.getKey(), entry.getValue());
				}
			}
		}
		// Varps that read empty may just not be sent yet (e.g. at login); a real emptying is seen when it's open
		if (pets.isEmpty() && hadPets)
		{
			return;
		}
		pets.forEach(pet -> quantities.put(pet, 1));
		if (latest != null && latest.getQuantities().equals(quantities))
		{
			return;
		}
		log.debug("Pet house varps: {} pets", pets.size());
		record(SnapshotLocation.PET_HOUSE, quantities);
	}

	/**
	 * @return the item of each pet living in the menagerie, or null if the game's pet list isn't loaded
	 */
	private Set<Integer> menageriePets()
	{
		EnumComposition petEnum = client.getEnum(MenageriePets.PETS_ENUM);
		if (petEnum == null)
		{
			return null;
		}
		int[] varps = {
			client.getVarpValue(VarPlayerID.PRAYER20),
			client.getVarpValue(VarPlayerID.MENAGERIE_CONTENTS2),
			client.getVarpValue(VarPlayerID.MENAGERIE_CONTENTS3),
		};
		return MenageriePets.read(varps, petEnum::getIntValue);
	}

	/**
	 * @return the item among the candidates with this name (any case), or -1
	 */
	private int itemNamed(String name, Set<Integer> candidates)
	{
		for (int itemId : candidates)
		{
			if (client.getItemDefinition(itemId).getName().equalsIgnoreCase(name))
			{
				return itemId;
			}
		}
		return -1;
	}

	/**
	 * @return the watering can item stored at the tool leprechaun for its varbit value, or -1
	 */
	private int wateringCan(int value)
	{
		EnumComposition cans = client.getEnum(VarbitStorage.WATERING_CANS_ENUM);
		return cans == null ? -1 : cans.getIntValue(value);
	}

	/**
	 * @return the container or varbit storage shown by this interface group, or null
	 */
	private static SnapshotLocation interfaceStorage(int interfaceId)
	{
		ContainerStorage container = ContainerStorage.forInterface(interfaceId);
		if (container != null)
		{
			return container.location;
		}
		VarbitStorage varbits = VarbitStorage.forInterface(interfaceId);
		return varbits == null ? null : varbits.location;
	}

	/**
	 * @return whether the location is a container or varbit storage (its visits end when its interface closes)
	 */
	private static boolean isInterfaceStorage(SnapshotLocation location)
	{
		if (ContainerStorage.forLocation(location) != null)
		{
			return true;
		}
		for (VarbitStorage storage : VarbitStorage.values())
		{
			if (storage.location == location)
			{
				return true;
			}
		}
		return false;
	}

	private void scanPoh()
	{
		PohStorageScanner.Result result = pohScanner.scan();
		if (result != null)
		{
			pohScanDelay = -1;
			if (pohVisit != null && pohVisit != result.getLocation())
			{
				visitEntries.remove(pohVisit);
			}
			pohVisit = result.getLocation();
			log.debug("Scanned {}: {} items stored, tier text '{}'", result.getLocation(), result.getQuantities().size(),
				result.getTierText());
			record(result.getLocation(), result.getQuantities());
		}
		else if (--pohScanAttemptsLeft > 0)
		{
			pohScanDelay = 1;
		}
		else
		{
			pohScanDelay = -1;
			log.debug("Couldn't recognise the POH storage interface");
		}
	}

	private void record(SnapshotLocation location, Map<Integer, Integer> quantities)
	{
		load();
		if (loadedProfile == null)
		{
			return;
		}

		Snapshot snapshot = new Snapshot(quantities, System.currentTimeMillis());
		List<Snapshot> history = histories.computeIfAbsent(location, k -> new ArrayList<>());
		if (SnapshotHistory.record(history, snapshot, visitEntries.contains(location),
			config.bankSearchSnapshotCount()))
		{
			visitEntries.add(location);
		}
		else
		{
			visitEntries.remove(location);
		}

		configManager.setRSProfileConfiguration(UltimateBankConfig.GROUP, key(location), gson.toJson(history));
		notifyListeners(location);
	}

	/**
	 * @return every location's latest snapshot
	 */
	public Map<SnapshotLocation, Snapshot> latestAll()
	{
		Map<SnapshotLocation, Snapshot> latest = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : histories.keySet())
		{
			latest.put(location, latest(location));
		}
		return Collections.unmodifiableMap(latest);
	}

	private static String key(SnapshotLocation location)
	{
		return KEY_PREFIX + location.name().toLowerCase(Locale.ROOT);
	}

	private void notifyListeners(SnapshotLocation location)
	{
		listeners.forEach(listener -> listener.accept(location));
	}
}

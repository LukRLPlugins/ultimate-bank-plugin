package com.ultimatebank.core.snapshots;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ultimatebank.UltimateBankConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
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
	private final Client client;
	private final ConfigManager configManager;
	private final UltimateBankConfig config;
	private final Gson gson;
	private final PohStorageScanner pohScanner;

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
	/** Ticks left to keep re-reading the menagerie: its pet list fills in after it opens */
	private int menagerieRescans;

	@Inject
	StorageSnapshots(Client client, ConfigManager configManager, UltimateBankConfig config, Gson gson,
		PohStorageScanner pohScanner)
	{
		this.client = client;
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
		this.pohScanner = pohScanner;
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
		if (event.getContainerId() == InventoryID.BANK)
		{
			// Read it next tick, once the bank has finished updating
			bankScanPending = true;
		}
		else if (ContainerStorage.forInventory(event.getContainerId()) != null)
		{
			storageScans.add(ContainerStorage.forInventory(event.getContainerId()).location);
		}
		else if (event.getContainerId() == InventoryID.INV && openStorages.contains(SnapshotLocation.PET_HOUSE))
		{
			// Taking a pet out (or putting one in) changes the inventory, not the menagerie's container
			menagerieRescans = 2;
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

		SnapshotLocation storage = interfaceStorage(event.getGroupId());
		if (storage != null)
		{
			openStorages.add(storage);
			readThisOpen.remove(storage);
			// Its contents may already be loaded and not change, so read it anyway
			storageScans.add(storage);
			if (storage == SnapshotLocation.PET_HOUSE)
			{
				menagerieRescans = 4;
			}
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
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (bankScanPending)
		{
			bankScanPending = false;
			scanBank();
		}

		if (pohScanDelay > 0)
		{
			pohScanDelay--;
		}
		else if (pohScanDelay == 0)
		{
			scanPoh();
		}

		if (menagerieRescans > 0)
		{
			menagerieRescans--;
			storageScans.add(SnapshotLocation.PET_HOUSE);
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
		menagerieRescans = 0;
	}

	private void scanBank()
	{
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		// Only trust the container while the bank is open (or it changed just as it closed)
		if (bank == null || !bankVisit && client.getWidget(InterfaceID.Bankmain.ITEMS) == null)
		{
			return;
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
		if (quantities == null)
		{
			return;
		}

		// Re-reads while it's open often find nothing new; save unchanged contents once per opening (that notes when
		// it was seen), not on every re-read
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
		// The menagerie's container may not exist when it holds no pet items, but its pets are still listed
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
			Map<Integer, Integer> pets = pohScanner.scanMenagerie();
			if (pets != null)
			{
				pets.forEach(quantities::putIfAbsent);
			}
		}
		return quantities;
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

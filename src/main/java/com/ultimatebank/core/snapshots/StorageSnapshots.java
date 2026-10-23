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
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
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
	/** Container storages whose interface is open */
	private final Set<ContainerStorage> openContainers = EnumSet.noneOf(ContainerStorage.class);
	/** Container storages to read next tick */
	private final Set<ContainerStorage> containerScans = EnumSet.noneOf(ContainerStorage.class);
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
			containerScans.add(ContainerStorage.forInventory(event.getContainerId()));
		}
		else if (event.getContainerId() == InventoryID.INV && openContainers.contains(ContainerStorage.PET_HOUSE))
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

		ContainerStorage storage = ContainerStorage.forInterface(event.getGroupId());
		if (storage != null)
		{
			openContainers.add(storage);
			// The container may already be loaded and not change, so read it anyway
			containerScans.add(storage);
			if (storage == ContainerStorage.PET_HOUSE)
			{
				menagerieRescans = 4;
			}
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		ContainerStorage storage = ContainerStorage.forInterface(event.getGroupId());
		if (storage != null)
		{
			openContainers.remove(storage);
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
			containerScans.add(ContainerStorage.PET_HOUSE);
		}
		for (ContainerStorage storage : containerScans)
		{
			scanContainer(storage);
		}
		containerScans.clear();
		for (ContainerStorage storage : ContainerStorage.values())
		{
			if (!openContainers.contains(storage))
			{
				visitEntries.remove(storage.location);
			}
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
		openContainers.clear();
		containerScans.clear();
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

	private void scanContainer(ContainerStorage storage)
	{
		ItemContainer container = client.getItemContainer(storage.inventoryId);
		// Only trust it while its interface is open (or it changed just as it closed). The menagerie's container
		// may not exist when it holds no pet items, but its pets are still listed
		if (container == null && storage != ContainerStorage.PET_HOUSE
			|| !openContainers.contains(storage) && !visitEntries.contains(storage.location))
		{
			return;
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

		// Re-reads during a visit often find nothing new; don't save those again
		Snapshot latest = latest(storage.location);
		if (latest != null && visitEntries.contains(storage.location) && latest.getQuantities().equals(quantities))
		{
			return;
		}
		log.debug("Read {}: {} items", storage.location, quantities.size());
		record(storage.location, quantities);
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

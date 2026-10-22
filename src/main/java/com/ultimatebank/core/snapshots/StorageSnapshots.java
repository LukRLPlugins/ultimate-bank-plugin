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
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;

/**
 * Remembers what's in the bank and each POH costume room storage, per account, so features can work before (or
 * without) the player opening them again. Keeps the last few snapshots of each location for "recently changed",
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
	/** Snapshots kept per location (the latest, plus a few earlier for comparisons) */
	private static final int HISTORY_SIZE = 5;

	private final Client client;
	private final ConfigManager configManager;
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

	@Inject
	StorageSnapshots(Client client, ConfigManager configManager, Gson gson, PohStorageScanner pohScanner)
	{
		this.client = client;
		this.configManager = configManager;
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
		if (SnapshotHistory.record(history, snapshot, visitEntries.contains(location), HISTORY_SIZE))
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

package com.ultimatebank.feature.storagesavings;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.SettingsOpener;
import com.ultimatebank.core.snapshots.Snapshot;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import com.ultimatebank.core.snapshots.StorageSnapshots;
import com.ultimatebank.feature.storagesavings.data.StorageLocation;
import com.ultimatebank.feature.storagesavings.stash.StashTracker;
import com.ultimatebank.feature.storagesavings.stash.StashUnit;
import com.ultimatebank.feature.storagesavings.tiers.PohStorage;
import com.ultimatebank.feature.storagesavings.tiers.PohTiers;
import com.ultimatebank.feature.storagesavings.tiers.TierPlanner;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Flags banked items that could be stored elsewhere (POH storage, tackle box, STASH units, ...) and ranks
 * storage locations by how many bank slots they would free. Ported from Wasted Bank Space; see
 * docs/features/storage-savings.md.
 */
@Slf4j
@Singleton
public class StorageSavingsFeature implements BankFeature
{
	private static final String KEY_PREFIX = "storageSavings";
	private static final String STASH_ROW = "STASH units";
	/** Tooltips list at most this many STASH units per item */
	private static final int MAX_STASH_IN_TOOLTIP = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private UltimateBankConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private StorageSavingsOverlay overlay;

	@Inject
	private StorageSavingsTagTab tagTab;

	@Inject
	private StorableTitlesOverlay titlesOverlay;

	@Inject
	private StorageSavingsState state;

	@Inject
	private StashTracker stashTracker;

	private StorageSavingsPanel panel;
	private NavigationButton navButton;

	@Inject
	private StorageSnapshots snapshots;

	@Inject
	private PohTiers pohTiers;

	@Inject
	private SettingsOpener settingsOpener;

	@Inject
	private CharacterSettings characterSettings;

	private final Consumer<SnapshotLocation> snapshotListener = this::onSnapshotsChanged;

	/** Clicks on a storable tab group title show only that group */
	private final MouseAdapter titleClicks = new MouseAdapter()
	{
		@Override
		public MouseEvent mousePressed(MouseEvent event)
		{
			return onTitleMouse(event, true);
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent event)
		{
			return onTitleMouse(event, false);
		}

		@Override
		public MouseEvent mouseClicked(MouseEvent event)
		{
			return onTitleMouse(event, false);
		}
	};

	/** Non-placeholder item IDs in the bank, or null until the bank has been seen */
	private Set<Integer> bankItems;
	/** When the saved bank snapshot behind {@link #bankItems} was taken, or 0 if the bank was seen live */
	private long bankSnapshotAt;
	/** Real Construction level the upgrade hints were worked out for */
	private int constructionLevel;
	/** Every storable item's ID by normalized name, built on first use */
	private Map<String, Set<Integer>> namesToIds;

	@Override
	public boolean isEnabled(UltimateBankConfig config)
	{
		return config.storageSavingsEnabled();
	}

	@Override
	public void startUp()
	{
		characterSettings.load();
		overlayManager.add(overlay);
		overlayManager.add(titlesOverlay);
		mouseManager.registerMouseListener(titleClicks);

		panel = new StorageSavingsPanel(settingsOpener::open);
		navButton = NavigationButton.builder()
			.tooltip("Ultimate Bank Savings")
			.icon(createIcon())
			.priority(8)
			.panel(panel)
			.build();
		syncPanel();

		tagTab.setBankOpen(client.getWidget(InterfaceID.Bankmain.ITEMS) != null);
		tagTab.sync();
		stashTracker.load();

		snapshots.addListener(snapshotListener);
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (bank != null)
		{
			readBank(bank);
		}
		recompute();
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(titlesOverlay);
		mouseManager.unregisterMouseListener(titleClicks);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		snapshots.removeListener(snapshotListener);
		tagTab.remove();
		state.set(StorageSavingsState.Results.EMPTY);
		bankItems = null;
		bankSnapshotAt = 0;
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.BANK)
		{
			if (readBank(event.getItemContainer()))
			{
				recompute();
			}
		}
		else if (config.storageSavingsStash())
		{
			stashTracker.onItemContainerChanged(event);
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		characterSettings.onConfigChanged(event);
		if (!UltimateBankConfig.GROUP.equals(event.getGroup()) || !event.getKey().startsWith(KEY_PREFIX))
		{
			return;
		}

		switch (event.getKey())
		{
			case "storageSavingsPanel":
				syncPanel();
				return;
			case "storageSavingsTagTab":
			case "storageSavingsTagTabGroups":
				clientThread.invokeLater(tagTab::sync);
				return;
			case "storageSavingsMarker":
			case "storageSavingsMarkerColor":
			case "storageSavingsMarkerStyle":
			case "storageSavingsMarkerCorner":
			case "storageSavingsMarkerSize":
			case "storageSavingsMarkerThickness":
				// read live by the overlay
				return;
			case "storageSavingsStash":
				clientThread.invokeLater(() ->
				{
					stashTracker.load();
					recompute();
				});
				return;
			default:
				// STASH state and tag tab bookkeeping are stored under this prefix too; recomputing for them is harmless
				clientThread.invokeLater(this::recompute);
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		// The new character's tiers, locations and STASH options
		characterSettings.load();
		bankItems = null;
		stashTracker.load();
		recompute();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			tagTab.setBankOpen(true);
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			tagTab.setBankOpen(false);
		}
	}

	/**
	 * Toggles the focus on a left-click on a storable tab title row, and keeps the game from seeing that click.
	 * The row is empty bank space, so the click would do nothing in game anyway.
	 */
	private MouseEvent onTitleMouse(MouseEvent event, boolean pressed)
	{
		if (event.getButton() != MouseEvent.BUTTON1)
		{
			return event;
		}
		String title = titlesOverlay.titleAt(event.getPoint());
		if (title == null)
		{
			return event;
		}
		if (pressed)
		{
			clientThread.invoke(() -> tagTab.toggleFocus(title));
		}
		event.consume();
		return event;
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		// A RuneLite profile switch replaced the settings: put the character's own back
		clientThread.invoke(characterSettings::load);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (bankItems == null || !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		MenuEntry[] entries = event.getMenuEntries();
		for (int i = entries.length - 1; i >= 0; i--)
		{
			Widget widget = entries[i].getWidget();
			if (widget == null || widget.getParentId() != InterfaceID.Bankmain.ITEMS)
			{
				continue;
			}

			int itemId = widget.getItemId();
			String name = itemManager.getItemComposition(itemId).getName();
			String ignored = config.storageSavingsIgnored();
			boolean listed = IgnoreList.containsExact(ignored, name, itemId);
			if (listed || state.getFlagged().containsKey(itemId))
			{
				client.getMenu().createMenuEntry(i)
					.setOption(listed ? "Unignore storable" : "Ignore storable")
					.setTarget(entries[i].getTarget())
					.setType(MenuAction.RUNELITE)
					.onClick(e -> config.storageSavingsIgnored(
						listed ? IgnoreList.remove(ignored, name, itemId) : IgnoreList.add(ignored, name)));
			}
			return;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		// Costume room furniture (or its empty build space) tells us which tier is built
		pohTiers.seen(event.getGameObject().getId());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (pohTiers.applySeen())
		{
			recompute();
		}

		if (config.storageSavingsStash() && stashTracker.onGameTick())
		{
			recompute();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// Upgrade hints only suggest tiers the player can build
		if (event.getSkill() == Skill.CONSTRUCTION && event.getLevel() != constructionLevel)
		{
			recompute();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (config.storageSavingsStash())
		{
			stashTracker.onGameStateChanged(event);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (config.storageSavingsStash())
		{
			stashTracker.onVarbitChanged(event);
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (config.storageSavingsStash())
		{
			stashTracker.onScriptPreFired(event);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (config.storageSavingsStash())
		{
			stashTracker.onMenuOptionClicked(event);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (config.storageSavingsStash())
		{
			stashTracker.onChatMessage(event);
			recompute();
		}
	}

	/**
	 * @return whether the bank's contents changed
	 */
	private boolean readBank(ItemContainer bank)
	{
		Set<Integer> items = new HashSet<>();
		for (Item item : bank.getItems())
		{
			int itemId = item.getId();
			if (itemId <= 0 || itemManager.getItemComposition(itemId).getPlaceholderTemplateId() != -1)
			{
				continue;
			}
			items.add(itemId);
		}

		boolean wasSnapshot = bankSnapshotAt != 0;
		bankSnapshotAt = 0;
		if (items.equals(bankItems) && !wasSnapshot)
		{
			return false;
		}
		bankItems = items;
		return true;
	}

	/**
	 * A bank or POH snapshot was recorded (location), or the account's snapshots were loaded (null).
	 */
	private void onSnapshotsChanged(SnapshotLocation location)
	{
		// A bank snapshot taken while we're watching the live bank is the data we already recomputed from
		boolean bankLive = bankItems != null && bankSnapshotAt == 0;
		if (location != SnapshotLocation.BANK || !bankLive)
		{
			recompute();
		}
	}

	/**
	 * Uses the saved bank snapshot until the bank is seen live this session, if enabled.
	 */
	private void useBankSnapshotIfNeeded()
	{
		boolean live = bankItems != null && bankSnapshotAt == 0;
		if (live)
		{
			return;
		}

		Snapshot bank = config.storageSavingsUseSnapshots() ? snapshots.latest(SnapshotLocation.BANK) : null;
		if (bank == null)
		{
			bankItems = null;
			bankSnapshotAt = 0;
			return;
		}

		Set<Integer> items = new HashSet<>();
		for (int itemId : bank.getQuantities().keySet())
		{
			if (itemManager.getItemComposition(itemId).getPlaceholderTemplateId() == -1)
			{
				items.add(itemId);
			}
		}
		bankItems = items;
		bankSnapshotAt = bank.getTakenAt();
	}

	/**
	 * Storage stacks, so copies already stored never stop another going in; this is just shown as information.
	 *
	 * @return how many of the item the location's snapshot shows stored, or 0 if none or unknown
	 */
	private int storedCount(StorageLocation location, int itemId)
	{
		SnapshotLocation snapshotLocation = location.getSnapshotLocation();
		Snapshot stored = snapshotLocation == null || !config.storageSavingsUseSnapshots()
			? null : snapshots.latest(snapshotLocation);
		return stored == null ? 0 : stored.getQuantities().getOrDefault(itemId, 0);
	}

	/**
	 * Recalculates flagged items and the ranking from the last seen bank. Must be called on the client thread.
	 */
	private void recompute()
	{
		useBankSnapshotIfNeeded();
		if (bankItems == null)
		{
			publish(StorageSavingsState.Results.EMPTY);
			return;
		}

		Set<Integer> ignored = IgnoreList.resolve(config.storageSavingsIgnored(), namesToIds());
		boolean keepBis = config.storageSavingsKeepBis();
		boolean stash = config.storageSavingsStash();
		boolean stashBuiltOnly = config.storageSavingsStashBuiltOnly();

		Map<Integer, List<String>> flagged = new HashMap<>();
		Map<String, List<Integer>> itemsByLocation = new LinkedHashMap<>();
		Set<String> pohLocations = new HashSet<>();
		Set<Integer> alsoStored = new HashSet<>();
		Map<StorageLocation, TierResult> tiers = planTiers(ignored, keepBis);

		for (int itemId : bankItems)
		{
			if (ignored.contains(itemId) || (keepBis && StorageLocation.isBis(itemId)))
			{
				continue;
			}

			List<String> where = new ArrayList<>();
			for (StorageLocation location : StorageLocation.forItem(itemId))
			{
				TierResult tier = tiers.get(location);
				if (location.isEnabled(config) && (tier == null || tier.fits.contains(itemId)))
				{
					int stored = storedCount(location, itemId);
					if (stored > 0)
					{
						alsoStored.add(itemId);
					}
					where.add(stored > 0 ? location.getDisplayName() + " (" + stored + " stored)" : location.getDisplayName());
					itemsByLocation.computeIfAbsent(location.getDisplayName(), k -> new ArrayList<>()).add(itemId);
					if (location.isPoh())
					{
						pohLocations.add(location.getDisplayName());
					}
				}
			}

			if (stash)
			{
				List<StashUnit> units = StashUnit.forItem(itemId).stream()
					.filter(unit -> !stashBuiltOnly || (stashTracker.isBuilt(unit) && !stashTracker.isFilled(unit)))
					.collect(Collectors.toList());
				if (!units.isEmpty())
				{
					units.stream().limit(MAX_STASH_IN_TOOLTIP).forEach(unit -> where.add(unit.getLocation()));
					if (units.size() > MAX_STASH_IN_TOOLTIP)
					{
						where.add("...and " + (units.size() - MAX_STASH_IN_TOOLTIP) + " more STASH units");
					}
					itemsByLocation.computeIfAbsent(STASH_ROW, k -> new ArrayList<>()).add(itemId);
				}
			}

			if (!where.isEmpty())
			{
				flagged.put(itemId, List.copyOf(where));
			}
		}

		// Storages that hold nothing now but would after an upgrade still get a row, for the upgrade hint
		Map<String, TierResult> tiersByName = new HashMap<>();
		tiers.forEach((location, result) ->
		{
			tiersByName.put(location.getDisplayName(), result);
			if (result.nextTierExtra > 0)
			{
				itemsByLocation.computeIfAbsent(location.getDisplayName(), k -> new ArrayList<>());
				pohLocations.add(location.getDisplayName());
			}
		});

		Map<Integer, String> names = new HashMap<>();
		List<StorageSavingsState.LocationSavings> ranking = itemsByLocation.entrySet().stream()
			.map(e ->
			{
				List<Integer> ids = new ArrayList<>(e.getValue());
				ids.forEach(id -> names.computeIfAbsent(id, k -> itemManager.getItemComposition(k).getName()));
				ids.sort(Comparator.comparing(names::get, String.CASE_INSENSITIVE_ORDER));
				TierResult tier = tiersByName.get(e.getKey());
				return new StorageSavingsState.LocationSavings(e.getKey(), pohLocations.contains(e.getKey()),
					ids.stream().map(names::get).collect(Collectors.toList()), List.copyOf(ids),
					tier == null ? null : tier.label, tier == null ? null : tier.nextTier,
					tier == null ? 0 : tier.nextTierExtra);
			})
			.sorted(Comparator.comparingInt((StorageSavingsState.LocationSavings l) -> l.getItemNames().size()).reversed()
				.thenComparing(Comparator.comparingInt(StorageSavingsState.LocationSavings::getNextTierExtra).reversed())
				.thenComparing(StorageSavingsState.LocationSavings::getName))
			.collect(Collectors.toList());

		log.debug("{} storable items in bank across {} locations", flagged.size(), ranking.size());
		Map<Integer, Integer> icons = MarkerIcons.byItem(ranking, StorageSavingsFeature::iconForLocation);
		publish(new StorageSavingsState.Results(true, bankSnapshotAt, alsoStored.size(), Map.copyOf(flagged), ranking, icons));
	}

	/**
	 * @return the corner icon item for a ranking row's name, or -1 if there's none
	 */
	private static int iconForLocation(String name)
	{
		if (STASH_ROW.equals(name))
		{
			return ItemID.TRAIL_CLUE_MASTER;
		}
		for (StorageLocation location : StorageLocation.values())
		{
			if (location.getDisplayName().equals(name))
			{
				return location.getIconItemId();
			}
		}
		return -1;
	}

	/**
	 * Applies each tiered POH storage's capacity at the built tier: which banked items fit, and what the best tier
	 * the player's Construction level can build would add.
	 */
	private Map<StorageLocation, TierResult> planTiers(Set<Integer> ignored, boolean keepBis)
	{
		Map<StorageLocation, TierResult> results = new HashMap<>();
		constructionLevel = client.getRealSkillLevel(Skill.CONSTRUCTION);
		for (PohStorage storage : PohStorage.values())
		{
			StorageLocation location = storage.getLocation();
			if (!location.isEnabled(config))
			{
				continue;
			}

			Set<Integer> candidates = new HashSet<>();
			for (int itemId : bankItems)
			{
				if (!ignored.contains(itemId) && !(keepBis && StorageLocation.isBis(itemId))
					&& StorageLocation.forItem(itemId).contains(location))
				{
					candidates.add(itemId);
				}
			}

			int tier = pohTiers.tier(storage);
			if (tier == PohTiers.UNKNOWN)
			{
				results.put(location, new TierResult(candidates, "tier unknown", null, 0));
				continue;
			}

			List<TierPlanner.StorageSet> sets = pohTiers.sets(storage);
			Snapshot snapshot = config.storageSavingsUseSnapshots() ? snapshots.latest(location.getSnapshotLocation()) : null;
			Set<Integer> stored = snapshot == null ? Set.of() : snapshot.getQuantities().keySet();
			List<PohStorage.Tier> storageTiers = storage.getTiers();

			Set<Integer> fits = tier == PohStorage.NOT_BUILT ? Set.of()
				: TierPlanner.fits(storage.getLimit(), storageTiers.get(tier).getCapacity(), sets, candidates, stored);
			String nextTier = null;
			int extra = 0;
			// Only an upgrade the player has the Construction level for can save space
			int upgrade = storage.bestUpgrade(tier, constructionLevel);
			if (upgrade != PohStorage.NOT_BUILT)
			{
				PohStorage.Tier next = storageTiers.get(upgrade);
				nextTier = next.getName();
				extra = TierPlanner.fits(storage.getLimit(), next.getCapacity(), sets, candidates, stored).size() - fits.size();
			}
			String label = tier == PohStorage.NOT_BUILT ? "not built" : storageTiers.get(tier).getName();
			results.put(location, new TierResult(fits, label, nextTier, Math.max(0, extra)));
		}
		return results;
	}

	@Value
	private static class TierResult
	{
		Set<Integer> fits;
		String label;
		String nextTier;
		int nextTierExtra;
	}

	private void publish(StorageSavingsState.Results results)
	{
		state.set(results);
		tagTab.setItems(results.getFlagged().keySet(), results.getRanking());
		StorageSavingsPanel panel = this.panel;
		if (panel != null)
		{
			SwingUtilities.invokeLater(() -> panel.update(results));
		}
	}

	private Map<String, Set<Integer>> namesToIds()
	{
		if (namesToIds == null)
		{
			Set<Integer> storable = new HashSet<>(StorageLocation.allItemIds());
			storable.addAll(StashUnit.allItemIds());
			Map<String, Set<Integer>> names = new HashMap<>();
			for (int itemId : storable)
			{
				String name = IgnoreList.normalize(itemManager.getItemComposition(itemId).getName());
				names.computeIfAbsent(name, k -> new HashSet<>()).add(itemId);
			}
			namesToIds = names;
		}
		return namesToIds;
	}

	private void syncPanel()
	{
		if (navButton == null)
		{
			return;
		}
		if (config.storageSavingsPanel())
		{
			clientToolbar.addNavigation(navButton);
		}
		else
		{
			clientToolbar.removeNavigation(navButton);
		}
	}

	/**
	 * A small chest icon, drawn rather than shipped as a PNG.
	 */
	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(120, 72, 32));
		g.fillRoundRect(1, 4, 14, 11, 3, 3);
		g.setColor(new Color(160, 100, 48));
		g.fillRoundRect(1, 2, 14, 5, 3, 3);
		g.setColor(new Color(255, 200, 0));
		g.fillRect(7, 6, 2, 4);
		g.dispose();
		return icon;
	}
}

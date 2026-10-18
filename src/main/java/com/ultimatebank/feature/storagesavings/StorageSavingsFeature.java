package com.ultimatebank.feature.storagesavings;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.ExtraTabs;
import com.ultimatebank.feature.storagesavings.data.StorageLocation;
import com.ultimatebank.feature.storagesavings.stash.StashTracker;
import com.ultimatebank.feature.storagesavings.stash.StashUnit;
import java.awt.Color;
import java.awt.Graphics2D;
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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
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
	private ClientToolbar clientToolbar;

	@Inject
	private StorageSavingsOverlay overlay;

	@Inject
	private StorageSavingsTagTab tagTab;

	@Inject
	private StorageSavingsState state;

	@Inject
	private StashTracker stashTracker;

	@Inject
	private ExtraTabs extraTabs;

	private final Runnable extraTabsListener = this::onExtraTabsChanged;
	/** Whether Sorted tabs was showing published tabs when we last looked */
	private boolean extraTabsDisplayed;

	private StorageSavingsPanel panel;
	private NavigationButton navButton;

	/** Non-placeholder item IDs in the bank, or null until the bank has been seen */
	private Set<Integer> bankItems;
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
		overlayManager.add(overlay);

		panel = new StorageSavingsPanel();
		navButton = NavigationButton.builder()
			.tooltip("Storage savings")
			.icon(createIcon())
			.priority(8)
			.panel(panel)
			.build();
		syncPanel();

		extraTabsDisplayed = extraTabs.isDisplayed();
		extraTabs.addListener(extraTabsListener);
		tagTab.sync();
		stashTracker.load();

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
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		extraTabs.removeListener(extraTabsListener);
		extraTabs.remove(StorageSavingsTagTab.TAG_NAME);
		tagTab.remove();
		state.set(StorageSavingsState.Results.EMPTY);
		bankItems = null;
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
				clientThread.invokeLater(() ->
				{
					tagTab.sync();
					recompute();
				});
				return;
			case "storageSavingsMarker":
			case "storageSavingsMarkerColor":
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
		bankItems = null;
		stashTracker.load();
		recompute();
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
	public void onGameTick(GameTick event)
	{
		if (config.storageSavingsStash() && stashTracker.onGameTick())
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

		if (items.equals(bankItems))
		{
			return false;
		}
		bankItems = items;
		return true;
	}

	/**
	 * Recalculates flagged items and the ranking from the last seen bank. Must be called on the client thread.
	 */
	private void recompute()
	{
		if (bankItems == null)
		{
			publish(StorageSavingsState.Results.EMPTY);
			publishTab(List.of());
			return;
		}

		Set<Integer> ignored = IgnoreList.resolve(config.storageSavingsIgnored(), namesToIds());
		boolean keepBis = config.storageSavingsKeepBis();
		boolean stash = config.storageSavingsStash();
		boolean stashBuiltOnly = config.storageSavingsStashBuiltOnly();

		Map<Integer, List<String>> flagged = new HashMap<>();
		Map<String, List<Integer>> itemsByLocation = new LinkedHashMap<>();
		Set<String> pohLocations = new HashSet<>();

		for (int itemId : bankItems)
		{
			if (ignored.contains(itemId) || (keepBis && StorageLocation.isBis(itemId)))
			{
				continue;
			}

			List<String> where = new ArrayList<>();
			for (StorageLocation location : StorageLocation.forItem(itemId))
			{
				if (location.isEnabled(config))
				{
					where.add(location.getDisplayName());
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

		// Each location's items sorted by name, biggest location first
		Map<Integer, String> names = new HashMap<>();
		List<Map.Entry<String, List<Integer>>> locations = new ArrayList<>(itemsByLocation.entrySet());
		for (Map.Entry<String, List<Integer>> location : locations)
		{
			location.getValue().forEach(id -> names.computeIfAbsent(id, k -> itemManager.getItemComposition(k).getName()));
			location.getValue().sort(Comparator.comparing(names::get, String.CASE_INSENSITIVE_ORDER));
		}
		locations.sort(Comparator.comparingInt((Map.Entry<String, List<Integer>> e) -> e.getValue().size()).reversed()
			.thenComparing(Map.Entry::getKey));

		List<StorageSavingsState.LocationSavings> ranking = new ArrayList<>();
		List<ExtraTabs.Section> sections = new ArrayList<>();
		for (Map.Entry<String, List<Integer>> location : locations)
		{
			ranking.add(new StorageSavingsState.LocationSavings(location.getKey(), pohLocations.contains(location.getKey()),
				location.getValue().stream().map(names::get).collect(Collectors.toList())));
			sections.add(new ExtraTabs.Section(location.getKey(), List.copyOf(location.getValue())));
		}
		publishTab(sections);

		log.debug("{} storable items in bank across {} locations", flagged.size(), ranking.size());
		publish(new StorageSavingsState.Results(true, Map.copyOf(flagged), ranking));
	}

	/**
	 * Offers the storable items, one section per location, for Sorted tabs to show as "storable (ub)".
	 */
	private void publishTab(List<ExtraTabs.Section> sections)
	{
		if (config.storageSavingsTagTab())
		{
			extraTabs.publish(new ExtraTabs.Tab(StorageSavingsTagTab.TAG_NAME, StorageSavingsTagTab.ICON_ITEM_ID, sections));
		}
		else
		{
			extraTabs.remove(StorageSavingsTagTab.TAG_NAME);
		}
	}

	private void onExtraTabsChanged()
	{
		if (extraTabs.isDisplayed() != extraTabsDisplayed)
		{
			extraTabsDisplayed = extraTabs.isDisplayed();
			tagTab.sync();
		}
	}

	private void publish(StorageSavingsState.Results results)
	{
		state.set(results);
		tagTab.setItems(results.getFlagged().keySet());
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

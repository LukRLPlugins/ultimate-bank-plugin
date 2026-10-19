package com.ultimatebank.feature.storagesavings;

import com.google.common.base.Strings;
import com.ultimatebank.UltimateBankConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.util.Text;

/**
 * A virtual bank tag holding every flagged item, plus a tag tab to open it. Built on core bank tags:
 * the tag is registered with {@link TagManager}, and the tab is added to the Bank Tags plugin's tab list.
 * Adapted from the "wasted" tag tab by Brettgod1355 (Wasted Bank Space PR #136). When grouping is on, the tab gets a
 * core layout with one titled group per container (see {@link StorableLayout}).
 * All methods must be called on the client thread.
 */
@Slf4j
@Singleton
class StorageSavingsTagTab
{
	static final String TAG_NAME = "storable";

	private static final int ICON_ITEM_ID = ItemID.POH_FLATPACK_TREASURE_CHEST3;
	/** Set when we added the tab, so we only ever remove a tab we created and never a user's own */
	private static final String TAB_CREATED_KEY = "storageSavingsTagTabCreated";

	private final ConfigManager configManager;
	private final TagManager tagManager;
	private final BankTagsService bankTagsService;
	private final UltimateBankConfig config;
	private final LayoutManager layoutManager;

	// Replaced whole, never modified, so the registered tag always reads a consistent set
	private volatile Set<Integer> items = Collections.emptySet();
	private List<StorageSavingsState.LocationSavings> groups = Collections.emptyList();
	/** Titles by layout row, read by the overlay; empty when the tab isn't grouped */
	private volatile Map<Integer, String> titleRows = Collections.emptyMap();
	private boolean registered;

	@Inject
	StorageSavingsTagTab(ConfigManager configManager, TagManager tagManager, BankTagsService bankTagsService,
		UltimateBankConfig config, LayoutManager layoutManager)
	{
		this.layoutManager = layoutManager;
		this.configManager = configManager;
		this.tagManager = tagManager;
		this.bankTagsService = bankTagsService;
		this.config = config;
	}

	/**
	 * Adds or removes the tag and tab to match config.
	 */
	void sync()
	{
		if (config.storageSavingsTagTab())
		{
			if (!registered)
			{
				tagManager.registerTag(TAG_NAME, itemId -> items.contains(itemId));
				registered = true;
			}
			addTab();
			saveLayout();
		}
		else
		{
			remove();
		}
		refreshIfOpen();
	}

	void remove()
	{
		if (registered)
		{
			tagManager.unregisterTag(TAG_NAME);
			registered = false;
		}
		removeTab();
		titleRows = Collections.emptyMap();
		if (TAG_NAME.equals(bankTagsService.getActiveTag()))
		{
			bankTagsService.closeBankTag();
		}
	}

	/**
	 * @param ranking containers by bank slots freed, most first, with the flagged items each could hold
	 */
	void setItems(Set<Integer> flaggedItems, List<StorageSavingsState.LocationSavings> ranking)
	{
		if (flaggedItems.equals(items) && ranking.equals(groups))
		{
			return;
		}
		items = Set.copyOf(flaggedItems);
		groups = List.copyOf(ranking);
		if (registered)
		{
			saveLayout();
		}
		refreshIfOpen();
	}

	/**
	 * Titles to draw, by layout row, while the storable tab is open.
	 */
	Map<Integer, String> getTitleRows()
	{
		return TAG_NAME.equals(bankTagsService.getActiveTag()) ? titleRows : Collections.emptyMap();
	}

	private void saveLayout()
	{
		if (!config.storageSavingsTagTabGroups())
		{
			// Only remove a grouped layout we made, not one the user arranged by hand
			if (!titleRows.isEmpty())
			{
				layoutManager.removeLayout(TAG_NAME);
				titleRows = Collections.emptyMap();
			}
			return;
		}

		StorableLayout.Result result = StorableLayout.build(groups.stream()
			.map(g -> new StorableLayout.Group(g.getName(), g.getItemIds()))
			.collect(Collectors.toList()));
		layoutManager.saveLayout(new Layout(TAG_NAME, result.getPositions()));
		titleRows = result.getTitleRows();
	}

	private void addTab()
	{
		List<String> tabs = getTabNames();
		if (tabs.contains(TAG_NAME))
		{
			return;
		}

		tabs.add(TAG_NAME);
		configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_TABS_CONFIG, Text.toCSV(tabs));
		configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_ICON_PREFIX + TAG_NAME, ICON_ITEM_ID);
		configManager.setConfiguration(UltimateBankConfig.GROUP, TAB_CREATED_KEY, true);
		log.debug("Added bank tag tab '{}'", TAG_NAME);
	}

	private void removeTab()
	{
		if (!Boolean.TRUE.equals(configManager.getConfiguration(UltimateBankConfig.GROUP, TAB_CREATED_KEY, Boolean.class)))
		{
			return;
		}

		List<String> tabs = getTabNames();
		if (tabs.remove(TAG_NAME))
		{
			configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_TABS_CONFIG, Text.toCSV(tabs));
		}
		configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_ICON_PREFIX + TAG_NAME);
		layoutManager.removeLayout(TAG_NAME);
		configManager.unsetConfiguration(UltimateBankConfig.GROUP, TAB_CREATED_KEY);
		log.debug("Removed bank tag tab '{}'", TAG_NAME);
	}

	private List<String> getTabNames()
	{
		String tabs = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_TABS_CONFIG);
		return new ArrayList<>(Text.fromCSV(Strings.nullToEmpty(tabs)));
	}

	/**
	 * Re-opens the tag if it's being viewed, so the bank shows the current flagged items.
	 */
	private void refreshIfOpen()
	{
		if (registered && TAG_NAME.equals(bankTagsService.getActiveTag()))
		{
			bankTagsService.openBankTag(TAG_NAME, BankTagsService.OPTION_ALLOW_MODIFICATIONS);
		}
	}
}

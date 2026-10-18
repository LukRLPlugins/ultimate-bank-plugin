package com.ultimatebank.feature.storagesavings;

import com.google.common.base.Strings;
import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.ExtraTabs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.util.Text;

/**
 * A virtual bank tag holding every flagged item, plus a tag tab to open it. Built on core bank tags:
 * the tag is registered with {@link TagManager}, and the tab is added to the Bank Tags plugin's tab list.
 * Adapted from the "wasted" tag tab by Brettgod1355 (Wasted Bank Space PR #136).
 * All methods must be called on the client thread.
 */
@Slf4j
@Singleton
class StorageSavingsTagTab
{
	static final String TAG_NAME = "storable";

	static final int ICON_ITEM_ID = ItemID.POH_FLATPACK_TREASURE_CHEST3;
	/** Set when we added the tab, so we only ever remove a tab we created and never a user's own */
	private static final String TAB_CREATED_KEY = "storageSavingsTagTabCreated";

	private final ConfigManager configManager;
	private final TagManager tagManager;
	private final BankTagsService bankTagsService;
	private final UltimateBankConfig config;
	private final ExtraTabs extraTabs;

	// Replaced whole, never modified, so the registered tag always reads a consistent set
	private volatile Set<Integer> items = Collections.emptySet();
	private boolean registered;

	@Inject
	StorageSavingsTagTab(ConfigManager configManager, TagManager tagManager, BankTagsService bankTagsService,
		UltimateBankConfig config, ExtraTabs extraTabs)
	{
		this.extraTabs = extraTabs;
		this.configManager = configManager;
		this.tagManager = tagManager;
		this.bankTagsService = bankTagsService;
		this.config = config;
	}

	/**
	 * Adds or removes the tag and tab to match config. Hidden while Sorted tabs shows "storable (ub)" instead.
	 */
	void sync()
	{
		if (config.storageSavingsTagTab() && !extraTabs.isDisplayed())
		{
			if (!registered)
			{
				tagManager.registerTag(TAG_NAME, itemId -> items.contains(itemId));
				registered = true;
			}
			addTab();
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
		if (TAG_NAME.equals(bankTagsService.getActiveTag()))
		{
			bankTagsService.closeBankTag();
		}
	}

	void setItems(Set<Integer> flaggedItems)
	{
		if (flaggedItems.equals(items))
		{
			return;
		}
		items = Set.copyOf(flaggedItems);
		refreshIfOpen();
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

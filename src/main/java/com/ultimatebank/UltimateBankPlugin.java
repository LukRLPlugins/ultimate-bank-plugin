package com.ultimatebank;

import com.google.inject.Provides;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.FeatureManager;
import com.ultimatebank.feature.sortedtabs.SortedTabsFeature;
import com.ultimatebank.feature.storagesavings.StorageSavingsFeature;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;

@Slf4j
// Needed to inject core bank tag services (BankTagsService, TagManager)
@PluginDependency(BankTagsPlugin.class)
@PluginDescriptor(
	name = "Ultimate Bank",
	description = "An all-in-one bank toolkit: layouts, setups, value, search and storage hints",
	tags = {"bank", "tags", "layout", "value", "setups", "storage", "poh"}
)
public class UltimateBankPlugin extends Plugin
{
	/**
	 * Every feature in the plugin. Add new features here (order = start order).
	 */
	private static final List<Class<? extends BankFeature>> FEATURES = List.of(
		StorageSavingsFeature.class,
		SortedTabsFeature.class
	);

	@Inject
	private ClientThread clientThread;

	@Inject
	private FeatureManager featureManager;

	@Override
	protected void startUp()
	{
		featureManager.load(FEATURES);
		log.debug("Loaded {} features", FEATURES.size());
		clientThread.invokeLater(featureManager::sync);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invokeLater(featureManager::stopAll);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (UltimateBankConfig.GROUP.equals(event.getGroup()))
		{
			clientThread.invokeLater(featureManager::sync);
		}
	}

	@Provides
	UltimateBankConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UltimateBankConfig.class);
	}
}

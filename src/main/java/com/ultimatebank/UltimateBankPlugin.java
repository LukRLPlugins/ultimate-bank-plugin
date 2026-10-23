package com.ultimatebank;

import com.google.inject.Provides;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.FeatureManager;
import com.ultimatebank.core.FeatureRollout;
import com.ultimatebank.core.snapshots.StorageSnapshots;
import com.ultimatebank.feature.storagesavings.StorageSavingsFeature;
import com.ultimatebank.feature.banksearch.BankSearchFeature;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
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
	description = "Free up bank space and find anything you own: storable item markers, POH upgrade hints, and a bank and POH search",
	tags = {"bank", "storage", "poh", "costume", "space", "stash", "tackle box", "seed vault", "wasted", "search", "find", "history"}
)
public class UltimateBankPlugin extends Plugin
{
	/**
	 * Every feature in the plugin. Add new features here (order = start order).
	 */
	private static final List<Class<? extends BankFeature>> FEATURES = List.of(
		StorageSavingsFeature.class,
		BankSearchFeature.class
	);

	/**
	 * Enable keys of features added after v1.0.0, and of options that change what a click does: on for new installs,
	 * off for people updating (see {@link FeatureRollout}). Add every new feature's toggle here; never remove one.
	 */
	private static final List<String> NEW_FEATURE_TOGGLES = List.of(
		"bankSearchEnabled",
		"storageSavingsTagTabWithdrawAll"
	);

	@Inject
	private ClientThread clientThread;

	@Inject
	private FeatureManager featureManager;

	@Inject
	private FeatureRollout featureRollout;

	@Inject
	private EventBus eventBus;

	@Inject
	private StorageSnapshots storageSnapshots;

	@Override
	protected void startUp()
	{
		// Shared services run whatever features are on
		eventBus.register(storageSnapshots);
		clientThread.invokeLater(storageSnapshots::load);

		featureRollout.apply(NEW_FEATURE_TOGGLES);
		featureManager.load(FEATURES);
		log.debug("Loaded {} features", FEATURES.size());
		clientThread.invokeLater(featureManager::sync);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invokeLater(featureManager::stopAll);
		eventBus.unregister(storageSnapshots);
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

package com.ultimatebank;

import com.google.inject.Provides;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.FeatureManager;
import com.ultimatebank.core.snapshots.StorageSnapshots;
import com.ultimatebank.feature.storagesavings.StorageSavingsFeature;
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
	description = "Bank toolkit: see what could be stored outside your bank and which storage upgrades free the most space",
	tags = {"bank", "storage", "poh", "costume", "space", "stash", "tackle box", "seed vault", "wasted"}
)
public class UltimateBankPlugin extends Plugin
{
	/**
	 * Every feature in the plugin. Add new features here (order = start order).
	 */
	private static final List<Class<? extends BankFeature>> FEATURES = List.of(
		StorageSavingsFeature.class
		// WheresMyStuffFeature: search panel hidden until it's ready for release (code kept)
	);

	@Inject
	private ClientThread clientThread;

	@Inject
	private FeatureManager featureManager;

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

package com.ultimatebank.core;

import com.google.inject.Injector;
import com.ultimatebank.UltimateBankConfig;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

/**
 * Starts and stops {@link BankFeature}s so that exactly the enabled ones are running.
 * All methods must be called on the client thread.
 */
@Slf4j
@Singleton
public class FeatureManager
{
	private final Injector injector;
	private final EventBus eventBus;
	private final UltimateBankConfig config;

	private final List<BankFeature> features = new ArrayList<>();
	private final Set<BankFeature> running = new LinkedHashSet<>();

	@Inject
	FeatureManager(Injector injector, EventBus eventBus, UltimateBankConfig config)
	{
		this.injector = injector;
		this.eventBus = eventBus;
		this.config = config;
	}

	public void load(List<Class<? extends BankFeature>> featureClasses)
	{
		features.clear();
		for (Class<? extends BankFeature> featureClass : featureClasses)
		{
			features.add(injector.getInstance(featureClass));
		}
	}

	/**
	 * Starts newly enabled features and stops newly disabled ones.
	 */
	public void sync()
	{
		for (BankFeature feature : features)
		{
			boolean enabled = feature.isEnabled(config);
			if (enabled && !running.contains(feature))
			{
				start(feature);
			}
			else if (!enabled && running.contains(feature))
			{
				stop(feature);
			}
		}
	}

	public void stopAll()
	{
		for (BankFeature feature : new ArrayList<>(running))
		{
			stop(feature);
		}
	}

	private void start(BankFeature feature)
	{
		log.debug("Starting feature {}", feature.getClass().getSimpleName());
		eventBus.register(feature);
		running.add(feature);
		try
		{
			feature.startUp();
		}
		catch (Exception e)
		{
			log.warn("Feature {} failed to start", feature.getClass().getSimpleName(), e);
			stop(feature);
		}
	}

	private void stop(BankFeature feature)
	{
		log.debug("Stopping feature {}", feature.getClass().getSimpleName());
		try
		{
			feature.shutDown();
		}
		catch (Exception e)
		{
			log.warn("Feature {} failed to shut down cleanly", feature.getClass().getSimpleName(), e);
		}
		eventBus.unregister(feature);
		running.remove(feature);
	}
}

package com.ultimatebank.core;

import static org.junit.Assert.assertEquals;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ultimatebank.UltimateBankConfig;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.inject.Singleton;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;

public class FeatureManagerTest
{
	@Singleton
	static class CountingFeature implements BankFeature
	{
		static boolean enabled = true;
		int starts;
		int stops;

		@Override
		public boolean isEnabled(UltimateBankConfig config)
		{
			return enabled;
		}

		@Override
		public void startUp()
		{
			starts++;
		}

		@Override
		public void shutDown()
		{
			stops++;
		}
	}

	@Test
	public void startsAndStopsFeaturesToMatchConfig()
	{
		// Test-only stand-in for the config proxy RuneLite would provide
		UltimateBankConfig config = (UltimateBankConfig) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[]{UltimateBankConfig.class}, (proxy, method, args) -> null);
		Injector injector = Guice.createInjector(binder ->
		{
			binder.bind(EventBus.class).toInstance(new EventBus());
			binder.bind(UltimateBankConfig.class).toInstance(config);
		});

		FeatureManager manager = injector.getInstance(FeatureManager.class);
		manager.load(List.of(CountingFeature.class));
		CountingFeature feature = injector.getInstance(CountingFeature.class);

		CountingFeature.enabled = true;
		manager.sync();
		manager.sync();
		assertEquals(1, feature.starts);

		CountingFeature.enabled = false;
		manager.sync();
		assertEquals(1, feature.stops);

		CountingFeature.enabled = true;
		manager.sync();
		manager.stopAll();
		assertEquals(2, feature.starts);
		assertEquals(2, feature.stops);
	}
}

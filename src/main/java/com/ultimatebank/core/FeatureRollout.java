package com.ultimatebank.core;

import com.ultimatebank.UltimateBankConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfile;

/**
 * New features ship on for new installs and off for people updating. A config item has one default for everyone, so
 * their toggles default to on and, the first time an update sees one, it's switched off if the plugin was already in
 * use. Remembers which toggles it has handled, so each is decided once.
 */
@Slf4j
@Singleton
public class FeatureRollout
{
	/** Comma-separated toggle keys already handled */
	private static final String SEEN_KEY = "featuresSeen";
	/** Saved by Storage savings when it first adds its tag tab (every v1.x install, unless that option was off) */
	private static final String TAG_TAB_CREATED_KEY = "storageSavingsTagTabCreated";

	private final ConfigManager configManager;

	@Inject
	FeatureRollout(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/**
	 * Call before features are started.
	 *
	 * @param toggles enable keys of features that should be off for existing installs
	 */
	public void apply(List<String> toggles)
	{
		String seen = configManager.getConfiguration(UltimateBankConfig.GROUP, SEEN_KEY);
		// Only data the plugin writes itself shows it was already in use: RuneLite saves every setting's default when
		// the plugin loads, so saved settings prove nothing. Storage savings records its tag tab on first start, and
		// per-account data (bank snapshots, detected tiers) lives in the RS profile store
		boolean tagTabCreated = configManager.getConfiguration(UltimateBankConfig.GROUP, TAG_TAB_CREATED_KEY) != null;
		boolean accountData = hasAccountData();
		boolean anyKeys = tagTabCreated || accountData;
		log.debug("Feature rollout: seen={}, tag tab created={}, account data={} ({} accounts known)", seen,
			tagTabCreated, accountData, configManager.getRSProfiles().size());
		// RuneLite saves a new setting's default (on) when the plugin loads, before this runs, so a saved "true" isn't
		// a choice. Only the user can have turned a toggle off
		Set<String> userSet = new HashSet<>();
		for (String toggle : toggles)
		{
			if ("false".equals(configManager.getConfiguration(UltimateBankConfig.GROUP, toggle)))
			{
				userSet.add(toggle);
			}
		}

		for (String toggle : toDisable(seen, anyKeys, toggles, userSet))
		{
			log.debug("Existing install: {} starts off", toggle);
			configManager.setConfiguration(UltimateBankConfig.GROUP, toggle, false);
		}

		Set<String> nowSeen = parse(seen);
		if (nowSeen.addAll(toggles) || seen == null)
		{
			configManager.setConfiguration(UltimateBankConfig.GROUP, SEEN_KEY, String.join(",", nowSeen));
		}
	}

	private boolean hasAccountData()
	{
		for (RuneScapeProfile profile : configManager.getRSProfiles())
		{
			if (!configManager.getRSProfileConfigurationKeys(UltimateBankConfig.GROUP, profile.getKey(), "").isEmpty())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @param seen    the saved list of handled toggles, or null if none has been saved yet
	 * @param anyKeys whether the plugin has saved data of its own (it was in use before)
	 * @param userSet toggles the user already turned off
	 * @return the toggles to switch off
	 */
	static List<String> toDisable(String seen, boolean anyKeys, List<String> toggles, Set<String> userSet)
	{
		boolean existingInstall = seen != null || anyKeys;
		Set<String> handled = parse(seen);
		List<String> off = new ArrayList<>();
		if (!existingInstall)
		{
			return off;
		}
		for (String toggle : toggles)
		{
			if (!handled.contains(toggle) && !userSet.contains(toggle))
			{
				off.add(toggle);
			}
		}
		return off;
	}

	private static Set<String> parse(String seen)
	{
		Set<String> keys = new LinkedHashSet<>();
		if (seen != null && !seen.isEmpty())
		{
			keys.addAll(Arrays.asList(seen.split(",")));
		}
		return keys;
	}
}

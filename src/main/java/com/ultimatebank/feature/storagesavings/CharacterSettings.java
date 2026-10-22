package com.ultimatebank.feature.storagesavings;

import com.ultimatebank.UltimateBankConfig;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;

/**
 * Keeps the settings that describe one character (its POH tiers, which storages it uses, STASH options) separate for
 * each character. RuneLite settings belong to the RuneLite profile, which several characters can share, so each
 * character's values are saved in its RS profile, and loaded into the settings when it logs in. The Configuration
 * panel therefore always shows the logged-in character's values. Must be called on the client thread.
 */
@Slf4j
@Singleton
class CharacterSettings
{
	/** Setting keys kept per character */
	static final List<String> KEYS = List.of(
		// POH tiers
		"storageSavingsTierFancyDressBox",
		"storageSavingsTierArmourCase",
		"storageSavingsTierMagicWardrobe",
		"storageSavingsTierCapeRack",
		"storageSavingsTierTreasureChest",
		// STASH options
		"storageSavingsStash",
		"storageSavingsStashBuiltOnly",
		// Locations
		"storageSavingsTreasureChest",
		"storageSavingsFancyDressBox",
		"storageSavingsMagicWardrobe",
		"storageSavingsArmourCase",
		"storageSavingsCapeRack",
		"storageSavingsToyBox",
		"storageSavingsBookcase",
		"storageSavingsPetHouse",
		"storageSavingsSpiceRack",
		"storageSavingsSeedVault",
		"storageSavingsToolLeprechaun",
		"storageSavingsTackleBox",
		"storageSavingsHuntsmansKit",
		"storageSavingsForestryKit",
		"storageSavingsSteelKeyRing",
		"storageSavingsMasterScrollBook",
		"storageSavingsFossilStorage",
		"storageSavingsElnockInquisitor",
		"storageSavingsFlamtaerBag",
		"storageSavingsNightmareZone",
		"storageSavingsMysteriousStranger"
	);

	/** RS profile key prefix for a character's copy of a setting */
	private static final String PREFIX = "character.";
	/** Saved for a setting left at its default (RuneLite doesn't store defaults) */
	static final String DEFAULT = "";

	private final ConfigManager configManager;
	/** Set while we write the settings ourselves, so those writes aren't saved back as the user's choice */
	private boolean applying;

	@Inject
	CharacterSettings(ConfigManager configManager)
	{
		this.configManager = configManager;
	}

	/**
	 * Loads the logged-in character's values into the settings. A character seen for the first time keeps the
	 * current settings (what it used before settings were per character), which are saved as its own.
	 */
	void load()
	{
		if (configManager.getRSProfileKey() == null)
		{
			return;
		}

		applying = true;
		try
		{
			int changed = 0;
			for (String key : KEYS)
			{
				String shared = configManager.getConfiguration(UltimateBankConfig.GROUP, key);
				String stored = configManager.getRSProfileConfiguration(UltimateBankConfig.GROUP, PREFIX + key);
				if (stored == null)
				{
					configManager.setRSProfileConfiguration(UltimateBankConfig.GROUP, PREFIX + key, toStored(shared));
					continue;
				}

				String wanted = fromStored(stored);
				if (!Objects.equals(wanted, shared))
				{
					changed++;
					if (wanted == null)
					{
						configManager.unsetConfiguration(UltimateBankConfig.GROUP, key);
					}
					else
					{
						configManager.setConfiguration(UltimateBankConfig.GROUP, key, wanted);
					}
				}
			}
			log.debug("Loaded per-character settings ({} differed)", changed);
		}
		finally
		{
			applying = false;
		}
	}

	/**
	 * Saves a per-character setting the user changed as the logged-in character's own.
	 */
	void onConfigChanged(ConfigChanged event)
	{
		if (applying || event.getProfile() != null || !UltimateBankConfig.GROUP.equals(event.getGroup())
			|| !KEYS.contains(event.getKey()) || configManager.getRSProfileKey() == null)
		{
			return;
		}
		configManager.setRSProfileConfiguration(UltimateBankConfig.GROUP, PREFIX + event.getKey(),
			toStored(event.getNewValue()));
	}

	/**
	 * @param value a setting's saved value, or null when it's at its default
	 */
	static String toStored(String value)
	{
		return value == null ? DEFAULT : value;
	}

	/**
	 * @return the setting value to apply, or null to reset it to its default
	 */
	static String fromStored(String stored)
	{
		return DEFAULT.equals(stored) ? null : stored;
	}
}

package com.ultimatebank.feature.storagesavings.tiers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ultimatebank.UltimateBankConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * The tier of each costume room storage (from config, else auto-detected per account) and each storage's sets.
 * All methods must be called on the client thread.
 */
@Slf4j
@Singleton
public class PohTiers
{
	/** Tier isn't set and hasn't been detected yet */
	public static final int UNKNOWN = TierChoice.AUTO;

	private static final String DETECTED_KEY_PREFIX = "storageSavingsTierDetected.";
	private static final String SETS_RESOURCE = "/com/ultimatebank/feature/storagesavings/storage-sets.json";

	private final ConfigManager configManager;
	private final UltimateBankConfig config;
	private final Gson gson;

	private Map<PohStorage, List<TierPlanner.StorageSet>> sets;
	/** Ticks with no new costume room objects before a house load counts as finished */
	private static final int QUIET_TICKS = 2;

	/** Tiers seen during the current house load; built furniture beats an empty build space */
	private final Map<PohStorage, Integer> pending = new EnumMap<>(PohStorage.class);
	private int quietTicksLeft;

	@Inject
	PohTiers(ConfigManager configManager, UltimateBankConfig config, Gson gson)
	{
		this.configManager = configManager;
		this.config = config;
		this.gson = gson;
	}

	/**
	 * @return the tier index in use, {@link PohStorage#NOT_BUILT}, or {@link #UNKNOWN}
	 */
	public int tier(PohStorage storage)
	{
		int chosen = choice(storage).tier();
		if (chosen != TierChoice.AUTO)
		{
			return chosen;
		}
		Integer detected = configManager.getRSProfileConfiguration(UltimateBankConfig.GROUP,
			DETECTED_KEY_PREFIX + storage.name(), Integer.class);
		return detected != null ? detected : UNKNOWN;
	}

	/**
	 * Notes a game object spawning in the player's house, if it's costume room furniture or its empty build space.
	 * The build space can spawn a tick before the built furniture, so {@link #applySeen()} waits for the load to end.
	 */
	public void seen(int objectId)
	{
		PohStorage storage = PohStorage.forObject(objectId);
		if (storage != null)
		{
			pending.merge(storage, PohStorage.tierForObject(objectId), Math::max);
			quietTicksLeft = QUIET_TICKS;
		}
	}

	/**
	 * Call once per tick. Saves the tiers seen once the house has stopped loading costume room objects.
	 *
	 * @return whether any storage's detected tier changed
	 */
	public boolean applySeen()
	{
		if (pending.isEmpty() || --quietTicksLeft > 0)
		{
			return false;
		}
		boolean changed = false;
		for (Map.Entry<PohStorage, Integer> entry : pending.entrySet())
		{
			PohStorage storage = entry.getKey();
			int tier = entry.getValue();
			String key = DETECTED_KEY_PREFIX + storage.name();
			Integer previous = configManager.getRSProfileConfiguration(UltimateBankConfig.GROUP, key, Integer.class);
			if (previous == null || previous != tier)
			{
				configManager.setRSProfileConfiguration(UltimateBankConfig.GROUP, key, tier);
				log.debug("Detected {} tier {}", storage, tier == PohStorage.NOT_BUILT ? "not built" : storage.getTiers().get(tier).getName());
				changed = true;
			}
		}
		pending.clear();
		return changed;
	}

	private TierChoice choice(PohStorage storage)
	{
		switch (storage)
		{
			case FANCY_DRESS_BOX:
				return config.storageSavingsTierFancyDressBox();
			case ARMOUR_CASE:
				return config.storageSavingsTierArmourCase();
			case MAGIC_WARDROBE:
				return config.storageSavingsTierMagicWardrobe();
			case CAPE_RACK:
				return config.storageSavingsTierCapeRack();
			default:
				return config.storageSavingsTierTreasureChest();
		}
	}

	/**
	 * @return the storage's sets (item IDs from storage-sets.json); our items in no wiki set count as their own set
	 */
	public List<TierPlanner.StorageSet> sets(PohStorage storage)
	{
		if (sets == null)
		{
			sets = loadSets();
		}
		return sets.getOrDefault(storage, Collections.emptyList());
	}

	private Map<PohStorage, List<TierPlanner.StorageSet>> loadSets()
	{
		Map<String, List<WikiSet>> raw;
		try (InputStream in = PohTiers.class.getResourceAsStream(SETS_RESOURCE))
		{
			raw = in == null ? null : gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<Map<String, List<WikiSet>>>()
				{
				}.getType());
		}
		catch (Exception e)
		{
			log.warn("Couldn't load {}", SETS_RESOURCE, e);
			raw = null;
		}

		Map<PohStorage, List<TierPlanner.StorageSet>> result = new EnumMap<>(PohStorage.class);
		for (PohStorage storage : PohStorage.values())
		{
			List<TierPlanner.StorageSet> storageSets = new ArrayList<>();
			Set<Integer> covered = new HashSet<>();
			for (WikiSet wiki : raw == null ? Collections.<WikiSet>emptyList() : raw.getOrDefault(storage.name(), Collections.emptyList()))
			{
				if (wiki.ids != null && !wiki.ids.isEmpty())
				{
					covered.addAll(wiki.ids);
					storageSets.add(new TierPlanner.StorageSet(wiki.set, new HashSet<>(wiki.ids), wiki.accomplishment, clueTier(wiki.tier)));
				}
			}
			// Items on our own lists that no wiki set covers count as their own set
			for (int itemId : storage.getLocation().getItemIds())
			{
				if (covered.add(itemId))
				{
					storageSets.add(new TierPlanner.StorageSet(String.valueOf(itemId), Set.of(itemId), false, -1));
				}
			}
			log.debug("{}: {} sets", storage, storageSets.size());
			result.put(storage, storageSets);
		}
		return result;
	}

	private static int clueTier(String tier)
	{
		if (tier == null)
		{
			return -1;
		}
		try
		{
			return PohStorage.ClueTier.valueOf(tier.toUpperCase(Locale.ROOT)).ordinal();
		}
		catch (IllegalArgumentException e)
		{
			return -1;
		}
	}

	/** One entry of storage-sets.json */
	private static final class WikiSet
	{
		String set;
		List<Integer> ids;
		String tier;
		boolean accomplishment;
	}
}

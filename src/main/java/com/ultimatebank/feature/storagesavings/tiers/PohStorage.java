package com.ultimatebank.feature.storagesavings.tiers;

import com.ultimatebank.feature.storagesavings.data.StorageLocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.gameval.ObjectID;

/**
 * Costume room storages whose capacity depends on the furniture tier built (OSRS wiki, see storage-sets.json).
 * Toy box and bookcase have no limit, so aren't listed.
 */
@Getter
public enum PohStorage
{
	FANCY_DRESS_BOX(StorageLocation.FANCY_DRESS_BOX, Limit.SETS, "costumes", List.of(
		new Tier("Oak", 2, 44, ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_OAK, ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_OPEN_OAK),
		new Tier("Teak", 4, 62, ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_TEAK, ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_OPEN_TEAK),
		new Tier("Mahogany", Tier.UNLIMITED, 80, ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_MAHOGANY, ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_OPEN_MAHOGANY))),
	ARMOUR_CASE(StorageLocation.ARMOUR_CASE, Limit.SETS, "sets", List.of(
		new Tier("Oak", 25, 46, ObjectID.POH_COS_ROOM_ARMOUR_CASE_OAK, ObjectID.POH_COS_ROOM_ARMOUR_CASE_OPEN_OAK),
		new Tier("Teak", 50, 64, ObjectID.POH_COS_ROOM_ARMOUR_CASE_TEAK, ObjectID.POH_COS_ROOM_ARMOUR_CASE_OPEN_TEAK),
		new Tier("Mahogany", Tier.UNLIMITED, 82, ObjectID.POH_COS_ROOM_ARMOUR_CASE_MAHOGANY, ObjectID.POH_COS_ROOM_ARMOUR_CASE_OPEN_MAHOGANY))),
	MAGIC_WARDROBE(StorageLocation.MAGIC_WARDROBE, Limit.SETS, "sets", List.of(
		new Tier("Oak", 7, 42, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OAK, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_OAK),
		new Tier("Carved oak", 14, 51, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_CARVED_OAK, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_CARVED_OAK),
		new Tier("Teak", 21, 60, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_TEAK, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_TEAK),
		new Tier("Carved teak", 28, 69, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_CARVED_TEAK, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_CARVED_TEAK),
		new Tier("Mahogany", 35, 78, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_MAHOGANY, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_MAHOGANY),
		new Tier("Gilded", 42, 87, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_MAHOGANY_GILDED, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_MAHOGANY_GILDED),
		new Tier("Marble", Tier.UNLIMITED, 96, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_MARBLE, ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_OPEN_MARBLE))),
	CAPE_RACK(StorageLocation.CAPE_RACK, Limit.ACCOMPLISHMENT_CAPES, "skillcapes", List.of(
		new Tier("Oak", 0, 54, ObjectID.POH_COS_ROOM_CAPE_RACK_OAK),
		new Tier("Teak", 1, 63, ObjectID.POH_COS_ROOM_CAPE_RACK_TEAK),
		new Tier("Mahogany", 5, 72, ObjectID.POH_COS_ROOM_CAPE_RACK_MAHOGANY),
		new Tier("Gilded", 10, 81, ObjectID.POH_COS_ROOM_CAPE_RACK_MAHOGANY_GILDED),
		new Tier("Marble", Tier.UNLIMITED, 90, ObjectID.POH_COS_ROOM_CAPE_RACK_MARBLE),
		new Tier("Magical", Tier.UNLIMITED, 99, ObjectID.POH_COS_ROOM_CAPE_RACK_MAGIC_STONE))),
	TREASURE_CHEST(StorageLocation.TREASURE_CHEST, Limit.CLUE_TIER, "clue tiers", List.of(
		new Tier("Oak", ClueTier.EASY.ordinal(), 48, ObjectID.POH_COS_ROOM_TRESURE_CHEST_OAK, ObjectID.POH_COS_ROOM_TRESURE_CHEST_OPEN_OAK),
		new Tier("Teak", ClueTier.MEDIUM.ordinal(), 66, ObjectID.POH_COS_ROOM_TRESURE_CHEST_TEAK, ObjectID.POH_COS_ROOM_TRESURE_CHEST_OPEN_TEAK),
		new Tier("Mahogany", ClueTier.MASTER.ordinal(), 84, ObjectID.POH_COS_ROOM_TRESURE_CHEST_MAHOGANY, ObjectID.POH_COS_ROOM_TRESURE_CHEST_OPEN_MAHOGANY)));

	/** What a tier's capacity means */
	public enum Limit
	{
		/** Capacity = how many sets (costumes, outfits) fit */
		SETS,
		/** Every cape fits; capacity = how many capes of accomplishment fit */
		ACCOMPLISHMENT_CAPES,
		/** Capacity = highest {@link ClueTier} whose rewards fit */
		CLUE_TIER
	}

	public enum ClueTier
	{
		BEGINNER, EASY, MEDIUM, HARD, ELITE, MASTER
	}

	@Value
	public static class Tier
	{
		public static final int UNLIMITED = Integer.MAX_VALUE;

		String name;
		int capacity;
		/** Construction level needed to build it (OSRS wiki, Costume room) */
		int level;
		int[] objectIds;

		Tier(String name, int capacity, int level, int... objectIds)
		{
			this.name = name;
			this.capacity = capacity;
			this.level = level;
			this.objectIds = objectIds;
		}
	}

	/** Empty build spaces (only visible in building mode): seeing one means the storage isn't built */
	private static final Map<Integer, PohStorage> STORAGE_BY_HOTSPOT = Map.of(
		ObjectID.POH_COS_ROOM_FANCY_DRESS_BOX_HOTSPOT, FANCY_DRESS_BOX,
		ObjectID.POH_COS_ROOM_ARMOUR_CASE_HOTSPOT, ARMOUR_CASE,
		ObjectID.POH_COS_ROOM_MAGIC_WARDROBE_HOTSPOT, MAGIC_WARDROBE,
		ObjectID.POH_COS_ROOM_CAPE_RACK_HOTSPOT, CAPE_RACK,
		ObjectID.POH_COS_ROOM_TRESURE_CHEST_HOTSPOT, TREASURE_CHEST);
	private static final Map<Integer, PohStorage> STORAGE_BY_OBJECT = new HashMap<>();
	private static final Map<Integer, Integer> TIER_BY_OBJECT = new HashMap<>();

	static
	{
		for (PohStorage storage : values())
		{
			for (int t = 0; t < storage.tiers.size(); t++)
			{
				for (int objectId : storage.tiers.get(t).getObjectIds())
				{
					STORAGE_BY_OBJECT.put(objectId, storage);
					TIER_BY_OBJECT.put(objectId, t);
				}
			}
		}
	}

	private final StorageLocation location;
	private final Limit limit;
	/** What's counted, for display, e.g. "costumes" */
	private final String unit;
	/** Lowest first */
	private final List<Tier> tiers;

	PohStorage(StorageLocation location, Limit limit, String unit, List<Tier> tiers)
	{
		this.location = location;
		this.limit = limit;
		this.unit = unit;
		this.tiers = tiers;
	}

	/**
	 * @return the storage this game object is (built furniture or its empty build space), or null
	 */
	public static PohStorage forObject(int objectId)
	{
		PohStorage storage = STORAGE_BY_OBJECT.get(objectId);
		return storage != null ? storage : STORAGE_BY_HOTSPOT.get(objectId);
	}

	/**
	 * @return the tier index this game object shows, or {@link #NOT_BUILT} for an empty build space
	 */
	public static int tierForObject(int objectId)
	{
		return TIER_BY_OBJECT.getOrDefault(objectId, NOT_BUILT);
	}

	/** Tier index meaning nothing is built */
	public static final int NOT_BUILT = -1;

	/**
	 * @return the highest tier above the built one that this Construction level can build, or {@link #NOT_BUILT}
	 * if there's none
	 */
	public int bestUpgrade(int builtTier, int constructionLevel)
	{
		int best = NOT_BUILT;
		for (int t = builtTier + 1; t < tiers.size(); t++)
		{
			if (tiers.get(t).getLevel() <= constructionLevel)
			{
				best = t;
			}
		}
		return best;
	}

	public static PohStorage forLocation(StorageLocation location)
	{
		for (PohStorage storage : values())
		{
			if (storage.location == location)
			{
				return storage;
			}
		}
		return null;
	}
}

package com.ultimatebank.core.snapshots;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

/**
 * Storages the game keeps as varbits, so they're known whenever you're logged in. Decoded the way the game's own
 * scripts do ([proc,farming_tools_getstored], [proc,fossil_storage_get]). Pure: varbits are passed in.
 */
enum VarbitStorage
{
	TOOL_LEPRECHAUN(SnapshotLocation.TOOL_LEPRECHAUN, InterfaceID.FARMING_TOOLS)
	{
		@Override
		Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans)
		{
			Map<Integer, Integer> items = new HashMap<>();
			// Tools: one bit, plus a count of extras that each hold two
			put(items, ItemID.RAKE, 2 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRARAKES)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_RAKE));
			put(items, ItemID.DIBBER, 2 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRADIBBERS)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_DIBBER));
			put(items, ItemID.SPADE, 2 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRASPADES)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_SPADE));
			put(items, varbits.applyAsInt(VarbitID.FARMING_TOOLS_FAIRYSECATEURS) == 1
					? ItemID.FAIRY_ENCHANTED_SECATEURS : ItemID.SECATEURS,
				2 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRASECATEURS)
					+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_SECATEURS));
			put(items, ItemID.GARDENING_TROWEL, 2 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRATROWELS)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_TROWEL));

			// The watering can's varbit is its charge level, mapped to an item by a game enum
			int can = varbits.applyAsInt(VarbitID.FARMING_TOOLS_WATERINGCAN);
			if (can > 0)
			{
				put(items, wateringCans.applyAsInt(can), 1);
			}

			int bottomless = varbits.applyAsInt(VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_TYPE);
			if (bottomless > 0)
			{
				put(items, bottomless == 1 ? ItemID.BOTTOMLESS_COMPOST_BUCKET : ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED, 1);
			}

			put(items, ItemID.PLANT_CURE, varbits.applyAsInt(VarbitID.FARMING_TOOLS_PLANTCURE));
			put(items, ItemID.BUCKET_EMPTY, 256 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRA2BUCKETS)
				+ 32 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRABUCKETS)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_BUCKETS));
			put(items, ItemID.BUCKET_COMPOST, 256 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRACOMPOST)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_COMPOST));
			put(items, ItemID.BUCKET_SUPERCOMPOST, 256 * varbits.applyAsInt(VarbitID.FARMING_TOOLS_EXTRASUPERCOMPOST)
				+ varbits.applyAsInt(VarbitID.FARMING_TOOLS_SUPERCOMPOST));
			put(items, ItemID.BUCKET_ULTRACOMPOST, varbits.applyAsInt(VarbitID.FARMING_TOOLS_ULTRACOMPOST));
			return items;
		}
	},
	FOSSIL_STORAGE(SnapshotLocation.FOSSIL_STORAGE, InterfaceID.FOSSIL_STORAGE)
	{
		@Override
		Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans)
		{
			Map<Integer, Integer> items = new HashMap<>();
			for (int[] fossil : FOSSILS)
			{
				put(items, fossil[1], varbits.applyAsInt(fossil[0]));
			}
			return items;
		}
	};

	/** Game enum mapping the stored watering can's varbit value to its item (enum_136 in the game's scripts) */
	static final int WATERING_CANS_ENUM = 136;

	/** Each fossil's varbit and item */
	private static final int[][] FOSSILS = {
		{VarbitID.FOSSIL_STORAGE_SMALL_UNID, ItemID.FOSSIL_SMALL_UNID},
		{VarbitID.FOSSIL_STORAGE_MEDIUM_UNID, ItemID.FOSSIL_MEDIUM_UNID},
		{VarbitID.FOSSIL_STORAGE_LARGE_UNID, ItemID.FOSSIL_LARGE_UNID},
		{VarbitID.FOSSIL_STORAGE_RARE_UNID, ItemID.FOSSIL_RARE_UNID},
		{VarbitID.FOSSIL_STORAGE_SMALL_1, ItemID.FOSSIL_SMALL_1},
		{VarbitID.FOSSIL_STORAGE_SMALL_2, ItemID.FOSSIL_SMALL_2},
		{VarbitID.FOSSIL_STORAGE_SMALL_3, ItemID.FOSSIL_SMALL_3},
		{VarbitID.FOSSIL_STORAGE_SMALL_4, ItemID.FOSSIL_SMALL_4},
		{VarbitID.FOSSIL_STORAGE_SMALL_5, ItemID.FOSSIL_SMALL_5},
		{VarbitID.FOSSIL_STORAGE_MEDIUM_1, ItemID.FOSSIL_MEDIUM_1},
		{VarbitID.FOSSIL_STORAGE_MEDIUM_2, ItemID.FOSSIL_MEDIUM_2},
		{VarbitID.FOSSIL_STORAGE_MEDIUM_3, ItemID.FOSSIL_MEDIUM_3},
		{VarbitID.FOSSIL_STORAGE_MEDIUM_4, ItemID.FOSSIL_MEDIUM_4},
		{VarbitID.FOSSIL_STORAGE_MEDIUM_5, ItemID.FOSSIL_MEDIUM_5},
		{VarbitID.FOSSIL_STORAGE_LARGE_1, ItemID.FOSSIL_LARGE_1},
		{VarbitID.FOSSIL_STORAGE_LARGE_2, ItemID.FOSSIL_LARGE_2},
		{VarbitID.FOSSIL_STORAGE_LARGE_3, ItemID.FOSSIL_LARGE_3},
		{VarbitID.FOSSIL_STORAGE_LARGE_4, ItemID.FOSSIL_LARGE_4},
		{VarbitID.FOSSIL_STORAGE_LARGE_5, ItemID.FOSSIL_LARGE_5},
		{VarbitID.FOSSIL_STORAGE_PLANT_1, ItemID.FOSSIL_PLANT_1},
		{VarbitID.FOSSIL_STORAGE_PLANT_2, ItemID.FOSSIL_PLANT_2},
		{VarbitID.FOSSIL_STORAGE_PLANT_3, ItemID.FOSSIL_PLANT_3},
		{VarbitID.FOSSIL_STORAGE_PLANT_4, ItemID.FOSSIL_PLANT_4},
		{VarbitID.FOSSIL_STORAGE_PLANT_5, ItemID.FOSSIL_PLANT_5},
		{VarbitID.FOSSIL_STORAGE_RARE_1, ItemID.FOSSIL_RARE_1},
		{VarbitID.FOSSIL_STORAGE_RARE_2, ItemID.FOSSIL_RARE_2},
		{VarbitID.FOSSIL_STORAGE_RARE_3, ItemID.FOSSIL_RARE_3},
		{VarbitID.FOSSIL_STORAGE_RARE_4, ItemID.FOSSIL_RARE_4},
		{VarbitID.FOSSIL_STORAGE_RARE_5, ItemID.FOSSIL_RARE_5},
		{VarbitID.FOSSIL_STORAGE_RARE_6, ItemID.FOSSIL_RARE_6},
	};

	final SnapshotLocation location;
	/** Interface group shown while it's open */
	final int interfaceId;

	VarbitStorage(SnapshotLocation location, int interfaceId)
	{
		this.location = location;
		this.interfaceId = interfaceId;
	}

	/**
	 * @param varbits      a varbit's current value
	 * @param wateringCans the watering can item for a FARMING_TOOLS_WATERINGCAN value
	 * @return item ID to quantity, without empty entries
	 */
	abstract Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans);

	/**
	 * @return the storage shown by this interface group, or null
	 */
	static VarbitStorage forInterface(int interfaceId)
	{
		for (VarbitStorage storage : values())
		{
			if (storage.interfaceId == interfaceId)
			{
				return storage;
			}
		}
		return null;
	}

	private static void put(Map<Integer, Integer> items, int itemId, int quantity)
	{
		if (itemId > 0 && quantity > 0)
		{
			items.merge(itemId, quantity, Integer::sum);
		}
	}
}

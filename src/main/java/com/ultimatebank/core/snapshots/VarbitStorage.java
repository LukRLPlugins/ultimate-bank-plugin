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
			return readTable(FOSSILS, varbits);
		}
	},
	MASTER_SCROLL_BOOK(SnapshotLocation.MASTER_SCROLL_BOOK, InterfaceID.BOOKOFSCROLLS)
	{
		@Override
		Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans)
		{
			Map<Integer, Integer> items = readTable(SCROLLS, varbits);
			// Watson's count is split over two varbits ([proc,bookofscrolls_watson_get])
			put(items, ItemID.TELEPORTSCROLL_WATSON, 256 * varbits.applyAsInt(VarbitID.BOOKOFSCROLLS_WATSON_HIGHBITS)
				+ varbits.applyAsInt(VarbitID.BOOKOFSCROLLS_WATSON_LOWBITS));
			return items;
		}
	},
	NIGHTMARE_ZONE(SnapshotLocation.NIGHTMARE_ZONE, InterfaceID.NZONE_REWARDS)
	{
		@Override
		Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans)
		{
			// Each barrel's varbit counts doses, recorded as 1-dose potions
			return readTable(NZONE_POTIONS, varbits);
		}
	},
	ELNOCK_INQUISITOR(SnapshotLocation.ELNOCK_INQUISITOR, InterfaceID.II_ELNOCK_STORAGE)
	{
		@Override
		Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans)
		{
			Map<Integer, Integer> items = new HashMap<>();
			// The net varbit says which net is stored, not how many
			int net = varbits.applyAsInt(VarbitID.II_STORED_NET);
			if (net == 1 || net == 2)
			{
				put(items, net == 1 ? ItemID.HUNTING_BUTTERFLY_NET : ItemID.II_MAGIC_BUTTERFLY_NET, 1);
			}
			put(items, ItemID.II_IMP_REPELLENT, varbits.applyAsInt(VarbitID.II_STORED_REPELLENT));
			put(items, ItemID.II_IMPLING_JAR, varbits.applyAsInt(VarbitID.II_STORED_IMPLING_JARS));
			return items;
		}
	},
	MYSTERIOUS_STRANGER(SnapshotLocation.MYSTERIOUS_STRANGER, InterfaceID.HALLOWED_TOOLS)
	{
		@Override
		Map<Integer, Integer> read(IntUnaryOperator varbits, IntUnaryOperator wateringCans)
		{
			return readTable(HALLOWED_TOOLS, varbits);
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

	/** Each teleport scroll's varbit and item ([proc,bookofscrolls_populate]); Watson's is read separately */
	private static final int[][] SCROLLS = {
		{VarbitID.BOOKOFSCROLLS_NARDAH, ItemID.TELEPORTSCROLL_NARDAH},
		{VarbitID.BOOKOFSCROLLS_DIGSITE, ItemID.TELEPORTSCROLL_DIGSITE},
		{VarbitID.BOOKOFSCROLLS_FELDIP, ItemID.TELEPORTSCROLL_FELDIP},
		{VarbitID.BOOKOFSCROLLS_LUNARISLE, ItemID.TELEPORTSCROLL_LUNARISLE},
		{VarbitID.BOOKOFSCROLLS_MORTTON, ItemID.TELEPORTSCROLL_MORTTON},
		{VarbitID.BOOKOFSCROLLS_PESTCONTROL, ItemID.TELEPORTSCROLL_PESTCONTROL},
		{VarbitID.BOOKOFSCROLLS_PISCATORIS, ItemID.TELEPORTSCROLL_PISCATORIS},
		{VarbitID.BOOKOFSCROLLS_TAIBWO, ItemID.TELEPORTSCROLL_TAIBWO},
		{VarbitID.BOOKOFSCROLLS_ELF, ItemID.TELEPORTSCROLL_ELF},
		{VarbitID.BOOKOFSCROLLS_MOSLES, ItemID.TELEPORTSCROLL_MOSLES},
		{VarbitID.BOOKOFSCROLLS_LUMBERYARD, ItemID.TELEPORTSCROLL_LUMBERYARD},
		{VarbitID.BOOKOFSCROLLS_ZULANDRA, ItemID.TELEPORTSCROLL_ZULANDRA},
		{VarbitID.BOOKOFSCROLLS_CERBERUS, ItemID.TELEPORTSCROLL_CERBERUS},
		{VarbitID.BOOKOFSCROLLS_REVENANTS, ItemID.TELEPORTSCROLL_REVENANTS},
		{VarbitID.BOOKOFSCROLLS_GUTHIXIAN_TEMPLE, ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE},
		{VarbitID.BOOKOFSCROLLS_SPIDERCAVE, ItemID.TELEPORTSCROLL_SPIDERCAVE},
		{VarbitID.BOOKOFSCROLLS_COLOSSAL_WYRM, ItemID.TELEPORTSCROLL_COLOSSAL_WYRM},
		{VarbitID.BOOKOFSCROLLS_CHASMOFFIRE, ItemID.TELEPORTSCROLL_CHASMOFFIRE},
		{VarbitID.BOOKOFSCROLLS_ARDEAGLAIS, ItemID.TELEPORTSCROLL_ARDEAGLAIS},
	};

	/** Each Nightmare Zone barrel's varbit (doses) and 1-dose potion ([proc,nzone_rewards_updatepotion]) */
	private static final int[][] NZONE_POTIONS = {
		{VarbitID.NZONE_POTION_1, ItemID.NZONE1DOSE2RANGERSPOTION},
		{VarbitID.NZONE_POTION_2, ItemID.NZONE1DOSE2MAGICPOTION},
		{VarbitID.NZONE_POTION_3, ItemID.NZONE1DOSEOVERLOADPOTION},
		{VarbitID.NZONE_POTION_4, ItemID.NZONE1DOSEABSORPTIONPOTION},
	};

	/** Each Hallowed Sepulchre tool's varbit and item ([proc,hallowed_tools_getstored]) */
	private static final int[][] HALLOWED_TOOLS = {
		{VarbitID.HALLOWED_STORAGE_TOKEN, ItemID.HALLOWED_TOKEN},
		{VarbitID.HALLOWED_STORAGE_GRAPPLE, ItemID.HALLOWED_GRAPPLE},
		{VarbitID.HALLOWED_STORAGE_FOCUS, ItemID.HALLOWED_FOCUS},
		{VarbitID.HALLOWED_STORAGE_SYMBOL, ItemID.HALLOWED_SYMBOL},
		{VarbitID.HALLOWED_STORAGE_HAMMER, ItemID.HALLOWED_HAMMER},
		{VarbitID.HALLOWED_STORAGE_RING, ItemID.HALLOWED_RING},
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

	/**
	 * @return the items of a table of {varbit, item} pairs, each varbit a count of its item
	 */
	private static Map<Integer, Integer> readTable(int[][] table, IntUnaryOperator varbits)
	{
		Map<Integer, Integer> items = new HashMap<>();
		for (int[] entry : table)
		{
			put(items, entry[1], varbits.applyAsInt(entry[0]));
		}
		return items;
	}

	private static void put(Map<Integer, Integer> items, int itemId, int quantity)
	{
		if (itemId > 0 && quantity > 0)
		{
			items.merge(itemId, quantity, Integer::sum);
		}
	}
}

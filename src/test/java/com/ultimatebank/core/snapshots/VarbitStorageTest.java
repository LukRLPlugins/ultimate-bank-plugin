package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

public class VarbitStorageTest
{
	private static final int WATERING_CAN_8 = 5340;
	private static final IntUnaryOperator CANS = value -> value == 9 ? WATERING_CAN_8 : -1;

	private static Map<Integer, Integer> leprechaun(Map<Integer, Integer> varbits)
	{
		return VarbitStorage.TOOL_LEPRECHAUN.read(id -> varbits.getOrDefault(id, 0), CANS);
	}

	@Test
	public void nothingStoredIsEmpty()
	{
		assertTrue(leprechaun(Map.of()).isEmpty());
		assertTrue(VarbitStorage.FOSSIL_STORAGE.read(id -> 0, CANS).isEmpty());
	}

	@Test
	public void toolsCountTheirBitPlusTwoPerExtra()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.FARMING_TOOLS_RAKE, 1);
		varbits.put(VarbitID.FARMING_TOOLS_EXTRARAKES, 2);
		varbits.put(VarbitID.FARMING_TOOLS_SPADE, 1);
		Map<Integer, Integer> items = leprechaun(varbits);
		assertEquals(5, (int) items.get(ItemID.RAKE));
		assertEquals(1, (int) items.get(ItemID.SPADE));
		assertNull(items.get(ItemID.DIBBER));
	}

	@Test
	public void magicSecateursReplaceNormalOnes()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.FARMING_TOOLS_SECATEURS, 1);
		varbits.put(VarbitID.FARMING_TOOLS_FAIRYSECATEURS, 1);
		Map<Integer, Integer> items = leprechaun(varbits);
		assertEquals(1, (int) items.get(ItemID.FAIRY_ENCHANTED_SECATEURS));
		assertNull(items.get(ItemID.SECATEURS));
	}

	@Test
	public void bucketsAndCompostCombineTheirCounters()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.FARMING_TOOLS_BUCKETS, 3);
		varbits.put(VarbitID.FARMING_TOOLS_EXTRABUCKETS, 2);
		varbits.put(VarbitID.FARMING_TOOLS_EXTRA2BUCKETS, 1);
		varbits.put(VarbitID.FARMING_TOOLS_COMPOST, 10);
		varbits.put(VarbitID.FARMING_TOOLS_EXTRACOMPOST, 1);
		varbits.put(VarbitID.FARMING_TOOLS_ULTRACOMPOST, 7);
		Map<Integer, Integer> items = leprechaun(varbits);
		assertEquals(256 + 64 + 3, (int) items.get(ItemID.BUCKET_EMPTY));
		assertEquals(266, (int) items.get(ItemID.BUCKET_COMPOST));
		assertEquals(7, (int) items.get(ItemID.BUCKET_ULTRACOMPOST));
	}

	@Test
	public void wateringCanAndBottomlessBucket()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.FARMING_TOOLS_WATERINGCAN, 9);
		varbits.put(VarbitID.FARMING_TOOLS_BOTTOMLESS_BUCKET_TYPE, 2);
		Map<Integer, Integer> items = leprechaun(varbits);
		assertEquals(1, (int) items.get(WATERING_CAN_8));
		assertEquals(1, (int) items.get(ItemID.BOTTOMLESS_COMPOST_BUCKET_FILLED));
	}

	@Test
	public void fossilsReadOneVarbitEach()
	{
		Map<Integer, Integer> items = VarbitStorage.FOSSIL_STORAGE.read(
			id -> id == VarbitID.FOSSIL_STORAGE_RARE_6 ? 2 : id == VarbitID.FOSSIL_STORAGE_SMALL_UNID ? 14 : 0, CANS);
		assertEquals(Map.of(ItemID.FOSSIL_RARE_6, 2, ItemID.FOSSIL_SMALL_UNID, 14), items);
	}

	@Test
	public void foundByInterface()
	{
		assertEquals(VarbitStorage.TOOL_LEPRECHAUN, VarbitStorage.forInterface(InterfaceID.FARMING_TOOLS));
		assertEquals(VarbitStorage.FOSSIL_STORAGE, VarbitStorage.forInterface(InterfaceID.FOSSIL_STORAGE));
		assertNull(VarbitStorage.forInterface(InterfaceID.BANKMAIN));
	}
}

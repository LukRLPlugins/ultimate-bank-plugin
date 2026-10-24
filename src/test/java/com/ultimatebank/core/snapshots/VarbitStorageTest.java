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
	public void storagesWithNothingAreEmpty()
	{
		for (VarbitStorage storage : VarbitStorage.values())
		{
			assertTrue(storage.toString(), storage.read(id -> 0, CANS).isEmpty());
		}
	}

	@Test
	public void scrollBookCountsEachScrollAndJoinsWatsonsBits()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.BOOKOFSCROLLS_NARDAH, 12);
		varbits.put(VarbitID.BOOKOFSCROLLS_ARDEAGLAIS, 3);
		varbits.put(VarbitID.BOOKOFSCROLLS_WATSON_HIGHBITS, 1);
		varbits.put(VarbitID.BOOKOFSCROLLS_WATSON_LOWBITS, 44);
		Map<Integer, Integer> items = VarbitStorage.MASTER_SCROLL_BOOK.read(id -> varbits.getOrDefault(id, 0), CANS);
		assertEquals(Map.of(ItemID.TELEPORTSCROLL_NARDAH, 12, ItemID.TELEPORTSCROLL_ARDEAGLAIS, 3,
			ItemID.TELEPORTSCROLL_WATSON, 300), items);
	}

	@Test
	public void nightmareZoneRecordsDosesAsOneDosePotions()
	{
		Map<Integer, Integer> items = VarbitStorage.NIGHTMARE_ZONE.read(
			id -> id == VarbitID.NZONE_POTION_3 ? 255 : id == VarbitID.NZONE_POTION_4 ? 1000 : 0, CANS);
		assertEquals(Map.of(ItemID.NZONE1DOSEOVERLOADPOTION, 255, ItemID.NZONE1DOSEABSORPTIONPOTION, 1000), items);
	}

	@Test
	public void elnockNetVarbitSaysWhichNet()
	{
		Map<Integer, Integer> varbits = new HashMap<>();
		varbits.put(VarbitID.II_STORED_NET, 2);
		varbits.put(VarbitID.II_STORED_IMPLING_JARS, 30);
		varbits.put(VarbitID.II_STORED_REPELLENT, 1);
		assertEquals(Map.of(ItemID.II_MAGIC_BUTTERFLY_NET, 1, ItemID.II_IMPLING_JAR, 30, ItemID.II_IMP_REPELLENT, 1),
			VarbitStorage.ELNOCK_INQUISITOR.read(id -> varbits.getOrDefault(id, 0), CANS));
		assertEquals(Map.of(ItemID.HUNTING_BUTTERFLY_NET, 1),
			VarbitStorage.ELNOCK_INQUISITOR.read(id -> id == VarbitID.II_STORED_NET ? 1 : 0, CANS));
	}

	@Test
	public void hallowedToolsReadOneVarbitEach()
	{
		Map<Integer, Integer> items = VarbitStorage.MYSTERIOUS_STRANGER.read(
			id -> id == VarbitID.HALLOWED_STORAGE_TOKEN ? 5 : id == VarbitID.HALLOWED_STORAGE_RING ? 1 : 0, CANS);
		assertEquals(Map.of(ItemID.HALLOWED_TOKEN, 5, ItemID.HALLOWED_RING, 1), items);
	}

	@Test
	public void foundByInterface()
	{
		assertEquals(VarbitStorage.TOOL_LEPRECHAUN, VarbitStorage.forInterface(InterfaceID.FARMING_TOOLS));
		assertEquals(VarbitStorage.FOSSIL_STORAGE, VarbitStorage.forInterface(InterfaceID.FOSSIL_STORAGE));
		assertEquals(VarbitStorage.MASTER_SCROLL_BOOK, VarbitStorage.forInterface(InterfaceID.BOOKOFSCROLLS));
		assertEquals(VarbitStorage.NIGHTMARE_ZONE, VarbitStorage.forInterface(InterfaceID.NZONE_REWARDS));
		assertEquals(VarbitStorage.ELNOCK_INQUISITOR, VarbitStorage.forInterface(InterfaceID.II_ELNOCK_STORAGE));
		assertEquals(VarbitStorage.MYSTERIOUS_STRANGER, VarbitStorage.forInterface(InterfaceID.HALLOWED_TOOLS));
		assertNull(VarbitStorage.forInterface(InterfaceID.BANKMAIN));
	}
}

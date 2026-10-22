package com.ultimatebank.feature.wheresmystuff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.Map;
import org.junit.Test;

public class SnapshotDiffTest
{
	@Test
	public void reportsGainsLossesAndNewOrGoneItems()
	{
		Map<Integer, Integer> before = Map.of(1, 10, 2, 5, 3, 1);
		Map<Integer, Integer> after = Map.of(1, 15, 2, 5, 4, 2);
		assertEquals(Map.of(1, 5, 3, -1, 4, 2), SnapshotDiff.changes(before, after));
	}

	@Test
	public void noPreviousMeansNoChanges()
	{
		assertEquals(Map.of(), SnapshotDiff.changes(null, Map.of(1, 1)));
	}

	@Test
	public void recognisesPohStoragesFromInterfaceText()
	{
		assertEquals(SnapshotLocation.TOY_BOX, SnapshotLocation.fromInterfaceText("Oak Toy box"));
		assertEquals(SnapshotLocation.FANCY_DRESS_BOX, SnapshotLocation.fromInterfaceText("Mahogany fancy dress box"));
		assertEquals(SnapshotLocation.ARMOUR_CASE, SnapshotLocation.fromInterfaceText("teak ARMOR case"));
		assertEquals(SnapshotLocation.MAGIC_WARDROBE, SnapshotLocation.fromInterfaceText("Gilded magic wardrobe"));
		assertNull(SnapshotLocation.fromInterfaceText("Deposit box"));
	}
}

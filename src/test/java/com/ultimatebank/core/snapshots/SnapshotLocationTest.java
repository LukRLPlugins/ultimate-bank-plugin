package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class SnapshotLocationTest
{
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

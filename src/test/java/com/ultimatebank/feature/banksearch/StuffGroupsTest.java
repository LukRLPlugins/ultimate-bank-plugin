package com.ultimatebank.feature.banksearch;

import static org.junit.Assert.assertEquals;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.List;
import org.junit.Test;

public class StuffGroupsTest
{
	private static final List<StuffRow> ROWS = List.of(
		new StuffRow(385, "Shark", 10, 1_000, SnapshotLocation.BANK),
		new StuffRow(995, "Coins", 5_000, 1, SnapshotLocation.BANK),
		new StuffRow(7_000, "Abyssal whip", 1, 1_500_000, SnapshotLocation.BANK),
		new StuffRow(8_000, "Bunny ears", 1, 0, SnapshotLocation.TOY_BOX));

	@Test
	public void groupsByLocationInLocationOrder()
	{
		List<StuffGroups.Group> groups = StuffGroups.group(ROWS, "", StuffGroups.Sort.VALUE);

		assertEquals(2, groups.size());
		assertEquals(SnapshotLocation.BANK, groups.get(0).getLocation());
		assertEquals(3, groups.get(0).getRows().size());
		assertEquals(1_515_000, groups.get(0).getTotalValue());
		assertEquals(SnapshotLocation.TOY_BOX, groups.get(1).getLocation());
	}

	@Test
	public void sortsByStackValueOrName()
	{
		assertEquals("Abyssal whip", StuffGroups.group(ROWS, "", StuffGroups.Sort.VALUE).get(0).getRows().get(0).getName());
		assertEquals("Shark", StuffGroups.group(ROWS, "", StuffGroups.Sort.VALUE).get(0).getRows().get(1).getName());
		assertEquals("Abyssal whip", StuffGroups.group(ROWS, "", StuffGroups.Sort.NAME).get(0).getRows().get(0).getName());
		assertEquals("Coins", StuffGroups.group(ROWS, "", StuffGroups.Sort.NAME).get(0).getRows().get(1).getName());
	}

	@Test
	public void searchMatchesItemNames()
	{
		List<StuffGroups.Group> groups = StuffGroups.group(ROWS, "SHAR", StuffGroups.Sort.VALUE);

		assertEquals(1, groups.size());
		assertEquals(1, groups.get(0).getRows().size());
		assertEquals(10_000, groups.get(0).getTotalValue());
	}

	@Test
	public void searchingALocationShowsAllItsItems()
	{
		List<StuffGroups.Group> groups = StuffGroups.group(ROWS, "toy box", StuffGroups.Sort.VALUE);

		assertEquals(1, groups.size());
		assertEquals(SnapshotLocation.TOY_BOX, groups.get(0).getLocation());
	}
}

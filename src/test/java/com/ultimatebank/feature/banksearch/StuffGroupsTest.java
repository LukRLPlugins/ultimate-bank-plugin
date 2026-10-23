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

	@Test
	public void sortsByQuantity()
	{
		List<StuffRow> rows = StuffGroups.group(ROWS, "", StuffGroups.Sort.QUANTITY).get(0).getRows();
		assertEquals("Coins", rows.get(0).getName());
		assertEquals("Shark", rows.get(1).getName());
		assertEquals("Abyssal whip", rows.get(2).getName());
	}

	@Test
	public void totalsAndShares()
	{
		assertEquals(1_515_000, StuffGroups.totalValue(ROWS));
		assertEquals("99.0%", StuffGroups.share(1_500_000, 1_515_000));
		assertEquals("<0.1%", StuffGroups.share(1, 1_515_000));
		assertEquals(null, StuffGroups.share(0, 1_515_000));
		assertEquals(null, StuffGroups.share(5, 0));
	}

	@Test
	public void highAlchValuesMoneyAtFaceValue()
	{
		assertEquals(1, ValueType.HIGH_ALCH.unitPrice(995, 1, 0));
		assertEquals(1_000, ValueType.HIGH_ALCH.unitPrice(13204, 1_000, 0));
		assertEquals(72_000, ValueType.HIGH_ALCH.unitPrice(4151, 1_500_000, 72_000));
		assertEquals(1_500_000, ValueType.GE.unitPrice(4151, 1_500_000, 72_000));
		assertEquals(30, ValueType.GE.unitPrice(8_000, 0, 30));
	}
}

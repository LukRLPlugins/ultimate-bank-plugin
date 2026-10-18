package com.ultimatebank.feature.layouttools;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LayoutMathTest
{
	private static final int[] NO_ITEMS = {};

	@Test
	public void zigzagPositionsAlternateRowsColumnByColumn()
	{
		assertEquals(0, LayoutMath.zigzagPosition(0));
		assertEquals(8, LayoutMath.zigzagPosition(1));
		assertEquals(1, LayoutMath.zigzagPosition(2));
		assertEquals(9, LayoutMath.zigzagPosition(3));
		assertEquals(15, LayoutMath.zigzagPosition(15));
		// 17th item starts the next pair of rows
		assertEquals(16, LayoutMath.zigzagPosition(16));
		assertEquals(24, LayoutMath.zigzagPosition(17));
	}

	@Test
	public void gearThenInventoryOnTheNextRowPair()
	{
		int[] gear = {100, -1, 101};
		int[] inv = {200, 201, -1, 202};
		int[] layout = LayoutMath.zigzag(gear, inv, null, NO_ITEMS, 4);

		assertEquals(100, layout[0]);
		assertEquals(101, layout[8]);
		// Gear used the first pair of rows (0-15), so inventory starts at row 2
		assertEquals(200, layout[16]);
		assertEquals(201, layout[24]);
		assertEquals(202, layout[17]);
	}

	@Test
	public void runesFollowInAPlainRow()
	{
		int[] layout = LayoutMath.zigzag(new int[]{100}, new int[]{200}, new int[]{300, 301}, NO_ITEMS, 4);
		// gear pair rows 0-1, inventory pair rows 2-3, runes on row 4
		assertEquals(300, layout[32]);
		assertEquals(301, layout[33]);
	}

	@Test
	public void duplicatesUpToTheLimitThenCollapse()
	{
		assertEquals(List.of(1, 1, 1, 2, 3), LayoutMath.limitDuplicates(List.of(1, 1, 1, 2, 3, 3, 3, 3, 3), 4));
		assertEquals(List.of(1, 2), LayoutMath.limitDuplicates(List.of(1, 1, 2), 1));
	}

	@Test
	public void zeroLimitMeansNoDuplicates()
	{
		int[] layout = LayoutMath.zigzag(NO_ITEMS, new int[]{5, 5, 5, 6}, null, NO_ITEMS, 0);
		assertEquals(2, Arrays.stream(layout).filter(id -> id != -1).count());
	}

	@Test
	public void existingItemsStayOrMoveOutOfTheWay()
	{
		int[] current = new int[40];
		Arrays.fill(current, -1);
		current[0] = 900; // will be taken by gear
		current[39] = 901; // untouched spot
		int[] layout = LayoutMath.zigzag(new int[]{100}, NO_ITEMS, null, current, 4);

		assertEquals(100, layout[0]);
		assertEquals(901, layout[39]);
		assertTrue("Displaced item is kept", Arrays.stream(layout).anyMatch(id -> id == 900));
	}

	@Test
	public void insertAndRemoveRows()
	{
		int[] layout = {1, -1, -1, -1, -1, -1, -1, -1, 2};
		int[] inserted = LayoutMath.insertRow(layout, 1);
		assertEquals(1, inserted[0]);
		assertTrue(LayoutMath.isRowEmpty(inserted, 1));
		assertEquals(2, inserted[16]);

		assertArrayEquals(layout, LayoutMath.removeRow(inserted, 1));
	}

	@Test
	public void removeRowRefusesRowsWithItems()
	{
		int[] layout = {1, -1, -1, -1, -1, -1, -1, -1, 2};
		assertArrayEquals(layout, LayoutMath.removeRow(layout, 0));
		assertFalse(LayoutMath.isRowEmpty(layout, 0));
		assertEquals(2, LayoutMath.rowCount(layout));
	}
}

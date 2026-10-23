package com.ultimatebank.feature.storagesavings;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class StorableLayoutTest
{
	@Test
	public void eachGroupGetsATitleRowThenItsItems()
	{
		StorableLayout.Result result = StorableLayout.build(List.of(
			new StorableLayout.Group("POH toy box", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9)),
			new StorableLayout.Group("Tackle box", List.of(10))));

		// Row 0 title, rows 1-2 toy box items, row 3 title, row 4 tackle box
		assertEquals(Map.of(0, "POH toy box", 3, "Tackle box"), result.getTitleRows());
		int[] p = result.getPositions();
		assertEquals(-1, p[0]);
		assertEquals(1, p[8]);
		assertEquals(9, p[16]);
		assertEquals(-1, p[17]);
		assertEquals(10, p[32]);
		assertEquals(33, p.length);
	}

	@Test
	public void emptyGroupsAreSkipped()
	{
		StorableLayout.Result result = StorableLayout.build(List.of(
			new StorableLayout.Group("Empty", List.of()),
			new StorableLayout.Group("Spice rack", List.of(5))));
		assertEquals(Map.of(0, "Spice rack"), result.getTitleRows());
		assertEquals(5, result.getPositions()[8]);
	}

	@Test
	public void itemInTwoContainersAppearsInBoth()
	{
		StorableLayout.Result result = StorableLayout.build(List.of(
			new StorableLayout.Group("A", List.of(7)),
			new StorableLayout.Group("B", List.of(7))));
		assertEquals(7, result.getPositions()[8]);
		assertEquals(7, result.getPositions()[24]);
	}

	@Test
	public void focusedGroupKeepsItsTitleWhenEmptied()
	{
		StorableLayout.Result result = StorableLayout.build(List.of(new StorableLayout.Group("Toy box", List.of())), true);
		assertEquals(Map.of(0, "Toy box"), result.getTitleRows());
		assertEquals(8, result.getPositions().length);
	}

	@Test
	public void keepOrderHoldsPreviousPlacesAndAppendsNewGroups()
	{
		List<StorableLayout.Group> ranking = List.of(
			new StorableLayout.Group("Tackle box", List.of(3)),
			new StorableLayout.Group("New", List.of(4)),
			new StorableLayout.Group("Toy box", List.of(1)),
			new StorableLayout.Group("Also new", List.of(5)));

		List<StorableLayout.Group> kept = StorableLayout.keepOrder(List.of("Toy box", "Gone", "Tackle box"), ranking);
		assertEquals(List.of("Toy box", "Tackle box", "New", "Also new"),
			kept.stream().map(StorableLayout.Group::getTitle).collect(Collectors.toList()));
	}
}

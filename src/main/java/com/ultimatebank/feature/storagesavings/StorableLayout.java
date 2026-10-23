package com.ultimatebank.feature.storagesavings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Lays out the storable tab: one group per container, each starting on a new row with a blank title row above it
 * (the title itself is drawn by {@link StorableTitlesOverlay}). An item that fits several containers appears in each
 * group. Pure: no client access.
 */
final class StorableLayout
{
	static final int ROW = 8;
	private static final int EMPTY = -1;

	private StorableLayout()
	{
	}

	@Value
	static class Group
	{
		String title;
		List<Integer> itemIds;
	}

	@Value
	static class Result
	{
		/** Item ID by bank position, -1 for empty */
		int[] positions;
		/** Title by the (blank) row it's drawn on */
		Map<Integer, String> titleRows;
	}

	static Result build(List<Group> groups)
	{
		return build(groups, false);
	}

	/**
	 * @param keepEmpty whether a group with no items still gets its title (a focused group the player has emptied)
	 */
	static Result build(List<Group> groups, boolean keepEmpty)
	{
		List<Integer> positions = new ArrayList<>();
		Map<Integer, String> titles = new LinkedHashMap<>();
		for (Group group : groups)
		{
			if (group.getItemIds().isEmpty() && !keepEmpty)
			{
				continue;
			}
			padToRow(positions);
			titles.put(positions.size() / ROW, group.getTitle());
			for (int i = 0; i < ROW; i++)
			{
				positions.add(EMPTY);
			}
			positions.addAll(group.getItemIds());
		}
		return new Result(positions.stream().mapToInt(Integer::intValue).toArray(), Collections.unmodifiableMap(titles));
	}

	/**
	 * Sorts groups by their place in a previous order, so groups don't jump around while the player withdraws from
	 * them. Groups not in it go after, in their own order.
	 *
	 * @param previous titles in the order they were shown
	 */
	static List<Group> keepOrder(List<String> previous, List<Group> groups)
	{
		List<Group> sorted = new ArrayList<>(groups);
		sorted.sort(Comparator.comparingInt(g ->
		{
			int index = previous.indexOf(g.getTitle());
			return index == -1 ? Integer.MAX_VALUE : index;
		}));
		return sorted;
	}

	private static void padToRow(List<Integer> positions)
	{
		while (positions.size() % ROW != 0)
		{
			positions.add(EMPTY);
		}
	}
}

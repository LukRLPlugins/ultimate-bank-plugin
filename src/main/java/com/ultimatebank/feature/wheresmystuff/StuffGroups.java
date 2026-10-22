package com.ultimatebank.feature.wheresmystuff;

import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Groups the panel's items by location, filtered by a search. Pure: no client access.
 */
final class StuffGroups
{
	private StuffGroups()
	{
	}

	enum Sort
	{
		VALUE("Value"),
		NAME("Name");

		private final String label;

		Sort(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@Value
	static class Group
	{
		SnapshotLocation location;
		/** Matching rows, sorted */
		List<StuffRow> rows;
		long totalValue;
	}

	/**
	 * @param query search text; matches an item's name, or a location's name (then all its items match). Blank = all.
	 * @return one group per location with matches, in location order
	 */
	static List<Group> group(List<StuffRow> rows, String query, Sort sort)
	{
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		Map<SnapshotLocation, List<StuffRow>> byLocation = rows.stream()
			.filter(r -> q.isEmpty()
				|| r.getName().toLowerCase(Locale.ROOT).contains(q)
				|| r.getLocation().getDisplayName().toLowerCase(Locale.ROOT).contains(q))
			.collect(Collectors.groupingBy(StuffRow::getLocation, () -> new EnumMap<>(SnapshotLocation.class),
				Collectors.toList()));

		List<Group> groups = new ArrayList<>();
		byLocation.forEach((location, matches) ->
		{
			matches.sort(comparator(sort));
			long value = matches.stream().mapToLong(StuffRow::totalValue).sum();
			groups.add(new Group(location, matches, value));
		});
		return groups;
	}

	static Comparator<StuffRow> comparator(Sort sort)
	{
		Comparator<StuffRow> byName = Comparator.comparing(StuffRow::getName, String.CASE_INSENSITIVE_ORDER);
		return sort == Sort.NAME ? byName
			: Comparator.comparingLong((StuffRow r) -> Math.abs(r.totalValue())).reversed().thenComparing(byName);
	}
}

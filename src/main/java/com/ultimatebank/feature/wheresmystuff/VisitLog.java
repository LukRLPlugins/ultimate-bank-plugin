package com.ultimatebank.feature.wheresmystuff;

import com.ultimatebank.core.snapshots.Snapshot;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * What each visit to a location changed, from its snapshots (one per visit). Pure: no client access.
 */
final class VisitLog
{
	private VisitLog()
	{
	}

	@Value
	static class Visit
	{
		SnapshotLocation location;
		long takenAt;
		/** Quantity change by item ID (negative = taken out) */
		Map<Integer, Integer> changes;
	}

	/**
	 * @param histories each location's snapshots, oldest first
	 * @return visits that changed something, newest first. A location's first snapshot has nothing to compare with,
	 * so it isn't a visit.
	 */
	static List<Visit> build(Map<SnapshotLocation, List<Snapshot>> histories)
	{
		List<Visit> visits = new ArrayList<>();
		histories.forEach((location, history) ->
		{
			for (int i = 1; i < history.size(); i++)
			{
				Snapshot visit = history.get(i);
				Map<Integer, Integer> changes = SnapshotDiff.changes(history.get(i - 1).getQuantities(), visit.getQuantities());
				if (!changes.isEmpty())
				{
					visits.add(new Visit(location, visit.getTakenAt(), changes));
				}
			}
		});
		visits.sort(Comparator.comparingLong(Visit::getTakenAt).reversed());
		return visits;
	}
}

package com.ultimatebank.feature.wheresmystuff;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * What changed between two snapshots of one location. Pure: no client access.
 */
final class SnapshotDiff
{
	private SnapshotDiff()
	{
	}

	/**
	 * @param previous quantities before, or null if there's no earlier snapshot (then nothing counts as changed)
	 * @return quantity change by item ID, only for items that changed (negative = fewer or gone)
	 */
	static Map<Integer, Integer> changes(Map<Integer, Integer> previous, Map<Integer, Integer> latest)
	{
		if (previous == null)
		{
			return Collections.emptyMap();
		}

		Set<Integer> ids = new HashSet<>(previous.keySet());
		ids.addAll(latest.keySet());
		Map<Integer, Integer> changes = new HashMap<>();
		for (int id : ids)
		{
			int delta = latest.getOrDefault(id, 0) - previous.getOrDefault(id, 0);
			if (delta != 0)
			{
				changes.put(id, delta);
			}
		}
		return changes;
	}
}

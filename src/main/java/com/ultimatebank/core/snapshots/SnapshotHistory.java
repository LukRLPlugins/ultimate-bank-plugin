package com.ultimatebank.core.snapshots;

import java.util.List;

/**
 * Updates a location's snapshot history (oldest first) so it holds one entry per visit.
 */
final class SnapshotHistory
{
	private SnapshotHistory()
	{
	}

	/**
	 * Records a snapshot taken during a visit.
	 *
	 * @param replaceLast the last entry was appended earlier in this same visit, so this snapshot replaces it
	 * @return whether the history's last entry now belongs to this visit (pass it as replaceLast next time)
	 */
	static boolean record(List<Snapshot> history, Snapshot snapshot, boolean replaceLast, int max)
	{
		if (replaceLast && !history.isEmpty())
		{
			history.remove(history.size() - 1);
		}

		boolean appended;
		if (!history.isEmpty() && history.get(history.size() - 1).sameContents(snapshot))
		{
			// Unchanged since the last visit: just note it was seen now
			history.set(history.size() - 1, snapshot);
			appended = false;
		}
		else
		{
			history.add(snapshot);
			appended = true;
		}
		while (history.size() > max)
		{
			history.remove(0);
		}
		return appended;
	}
}

package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SnapshotHistoryTest
{
	private static final int MAX = 5;

	private static Snapshot snap(int coins, long at)
	{
		return new Snapshot(Map.of(995, coins), at);
	}

	@Test
	public void changesWithinAVisitKeepOneEntry()
	{
		List<Snapshot> history = new ArrayList<>(List.of(snap(100, 1)));

		boolean visit = SnapshotHistory.record(history, snap(90, 2), false, MAX);
		visit = SnapshotHistory.record(history, snap(80, 3), visit, MAX);
		visit = SnapshotHistory.record(history, snap(70, 4), visit, MAX);

		assertTrue(visit);
		assertEquals(2, history.size());
		assertEquals(Integer.valueOf(100), history.get(0).getQuantities().get(995));
		assertEquals(Integer.valueOf(70), history.get(1).getQuantities().get(995));
		assertEquals(4, history.get(1).getTakenAt());
	}

	@Test
	public void aNewVisitAppends()
	{
		List<Snapshot> history = new ArrayList<>(List.of(snap(100, 1)));

		SnapshotHistory.record(history, snap(90, 2), false, MAX);
		SnapshotHistory.record(history, snap(80, 3), false, MAX);

		assertEquals(3, history.size());
	}

	@Test
	public void aVisitWithNoNetChangeOnlyRefreshesTheTimestamp()
	{
		List<Snapshot> history = new ArrayList<>(List.of(snap(100, 1)));

		boolean visit = SnapshotHistory.record(history, snap(100, 2), false, MAX);
		assertFalse(visit);
		visit = SnapshotHistory.record(history, snap(50, 3), visit, MAX);
		assertTrue(visit);
		visit = SnapshotHistory.record(history, snap(100, 4), visit, MAX);

		assertFalse(visit);
		assertEquals(1, history.size());
		assertEquals(4, history.get(0).getTakenAt());
	}

	@Test
	public void historyIsTrimmedToTheMaximum()
	{
		List<Snapshot> history = new ArrayList<>();
		for (int i = 0; i < MAX + 3; i++)
		{
			SnapshotHistory.record(history, snap(i, i), false, MAX);
		}

		assertEquals(MAX, history.size());
		assertEquals(Integer.valueOf(3), history.get(0).getQuantities().get(995));
	}
}

package com.ultimatebank.feature.wheresmystuff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.ultimatebank.core.snapshots.Snapshot;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class VisitLogTest
{
	private static final int SHARK = 385;
	private static final int COINS = 995;

	@Test
	public void eachVisitIsDiffedAgainstTheOneBefore()
	{
		Map<SnapshotLocation, List<Snapshot>> histories = new EnumMap<>(SnapshotLocation.class);
		histories.put(SnapshotLocation.BANK, List.of(
			new Snapshot(Map.of(SHARK, 10, COINS, 100), 1),
			new Snapshot(Map.of(SHARK, 7, COINS, 100), 2),
			new Snapshot(Map.of(SHARK, 7, COINS, 150), 3)));

		List<VisitLog.Visit> visits = VisitLog.build(histories);

		assertEquals(2, visits.size());
		// Newest first
		assertEquals(3, visits.get(0).getTakenAt());
		assertEquals(Map.of(COINS, 50), visits.get(0).getChanges());
		assertEquals(Map.of(SHARK, -3), visits.get(1).getChanges());
	}

	@Test
	public void theFirstSnapshotIsNotAVisit()
	{
		Map<SnapshotLocation, List<Snapshot>> histories = new EnumMap<>(SnapshotLocation.class);
		histories.put(SnapshotLocation.TOY_BOX, List.of(new Snapshot(Map.of(SHARK, 1), 1)));

		assertTrue(VisitLog.build(histories).isEmpty());
	}

	@Test
	public void visitsFromAllLocationsAreMergedByTime()
	{
		Map<SnapshotLocation, List<Snapshot>> histories = new EnumMap<>(SnapshotLocation.class);
		histories.put(SnapshotLocation.BANK, List.of(new Snapshot(Map.of(), 1), new Snapshot(Map.of(SHARK, 1), 5)));
		histories.put(SnapshotLocation.TOY_BOX, List.of(new Snapshot(Map.of(SHARK, 1), 2), new Snapshot(Map.of(), 4)));

		List<VisitLog.Visit> visits = VisitLog.build(histories);

		assertEquals(SnapshotLocation.BANK, visits.get(0).getLocation());
		assertEquals(SnapshotLocation.TOY_BOX, visits.get(1).getLocation());
		assertEquals(Map.of(SHARK, -1), visits.get(1).getChanges());
	}
}

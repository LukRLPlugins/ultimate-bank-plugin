package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SnapshotJsonTest
{
	@Test
	public void historyRoundTripsThroughJson()
	{
		// Test-only Gson; plugin code uses the injected one
		Gson gson = new Gson();
		List<Snapshot> history = List.of(new Snapshot(Map.of(995, 1000, 4151, 1), 123L));

		List<Snapshot> back = gson.fromJson(gson.toJson(history), new TypeToken<List<Snapshot>>()
		{
		}.getType());

		assertEquals(1, back.size());
		assertEquals(123L, back.get(0).getTakenAt());
		assertEquals(Integer.valueOf(1000), back.get(0).getQuantities().get(995));
		assertTrue(back.get(0).sameContents(history.get(0)));
	}
}

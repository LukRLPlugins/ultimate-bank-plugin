package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class PotionStorageReaderTest
{
	private static final int PRAYER_1 = 143;
	private static final int PRAYER_4 = 2434;
	private static final int UNF = 91;

	@Test
	public void dosesAreKeptAsOneDosePotions()
	{
		Map<Integer, Integer> items = new HashMap<>();
		PotionStorageReader.addDoses(items, dose -> dose == 1 ? PRAYER_1 : dose == 4 ? PRAYER_4 : -1, 1234);
		assertEquals(Map.of(PRAYER_1, 1234), items);
	}

	@Test
	public void unfinishedPotionsUseTheirOnlyForm()
	{
		Map<Integer, Integer> items = new HashMap<>();
		PotionStorageReader.addDoses(items, dose -> dose == 1 ? UNF : -1, 50);
		assertEquals(Map.of(UNF, 50), items);
	}

	@Test
	public void noDosesAddNothing()
	{
		Map<Integer, Integer> items = new HashMap<>();
		PotionStorageReader.addDoses(items, dose -> PRAYER_1, 0);
		PotionStorageReader.addDoses(items, dose -> -1, 10);
		assertTrue(items.isEmpty());
	}
}

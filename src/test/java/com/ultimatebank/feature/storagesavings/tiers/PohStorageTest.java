package com.ultimatebank.feature.storagesavings.tiers;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PohStorageTest
{
	// Fancy dress box: Oak 44, Teak 62, Mahogany 80

	@Test
	public void noUpgradeBelowTheNextTiersLevel()
	{
		assertEquals(PohStorage.NOT_BUILT, PohStorage.FANCY_DRESS_BOX.bestUpgrade(0, 61));
	}

	@Test
	public void upgradesToTheNextTierAtItsLevel()
	{
		assertEquals(1, PohStorage.FANCY_DRESS_BOX.bestUpgrade(0, 62));
	}

	@Test
	public void upgradesToTheHighestBuildableTier()
	{
		assertEquals(2, PohStorage.FANCY_DRESS_BOX.bestUpgrade(0, 99));
		// Magic wardrobe: Oak 42 ... Carved teak 69, Mahogany 78
		assertEquals(3, PohStorage.MAGIC_WARDROBE.bestUpgrade(0, 70));
	}

	@Test
	public void notBuiltUpgradesToTheFirstBuildableTier()
	{
		assertEquals(PohStorage.NOT_BUILT, PohStorage.FANCY_DRESS_BOX.bestUpgrade(PohStorage.NOT_BUILT, 43));
		assertEquals(0, PohStorage.FANCY_DRESS_BOX.bestUpgrade(PohStorage.NOT_BUILT, 50));
	}

	@Test
	public void topTierHasNoUpgrade()
	{
		assertEquals(PohStorage.NOT_BUILT, PohStorage.FANCY_DRESS_BOX.bestUpgrade(2, 99));
	}
}

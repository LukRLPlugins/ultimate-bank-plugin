package com.ultimatebank.feature.storagesavings.tiers;

import static org.junit.Assert.assertEquals;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class TierPlannerTest
{
	private static TierPlanner.StorageSet set(String name, Integer... ids)
	{
		return new TierPlanner.StorageSet(name, Set.of(ids), false, -1);
	}

	// Mime 5 pieces banked, Zombie 3, Frog 1
	private static final List<TierPlanner.StorageSet> COSTUMES = List.of(
		set("Mime", 1, 2, 3, 4, 5), set("Zombie", 10, 11, 12), set("Frog", 20), set("Camo", 30, 31));
	private static final Set<Integer> BANKED = Set.of(1, 2, 3, 4, 5, 10, 11, 12, 20);

	@Test
	public void limitedTierPicksTheSetsFreeingMostSlots()
	{
		// Oak fancy dress box: 2 costumes -> Mime (5) and Zombie (3), not Frog (1)
		assertEquals(Set.of(1, 2, 3, 4, 5, 10, 11, 12),
			TierPlanner.fits(PohStorage.Limit.SETS, 2, COSTUMES, BANKED, Set.of()));
	}

	@Test
	public void storedSetsKeepTheirSlotAndCountAgainstCapacity()
	{
		// Frog already stored (uses 1 of 2 slots): its banked piece fits, plus the biggest other set
		assertEquals(Set.of(20, 1, 2, 3, 4, 5),
			TierPlanner.fits(PohStorage.Limit.SETS, 2, COSTUMES, BANKED, Set.of(20)));
	}

	@Test
	public void fullStorageOnlyTakesStoredSets()
	{
		assertEquals(Set.of(), TierPlanner.fits(PohStorage.Limit.SETS, 1, COSTUMES, BANKED, Set.of(30)));
	}

	@Test
	public void unlimitedTakesEverything()
	{
		assertEquals(BANKED, TierPlanner.fits(PohStorage.Limit.SETS, PohStorage.Tier.UNLIMITED, COSTUMES, BANKED, Set.of()));
	}

	@Test
	public void capeRackOnlyLimitsCapesOfAccomplishment()
	{
		List<TierPlanner.StorageSet> capes = List.of(
			new TierPlanner.StorageSet("Attack cape", Set.of(1, 2), true, -1),
			new TierPlanner.StorageSet("Agility cape", Set.of(3), true, -1),
			new TierPlanner.StorageSet("Fire cape", Set.of(9), false, -1));
		// Oak rack: no skillcapes, but the fire cape fits
		assertEquals(Set.of(9), TierPlanner.fits(PohStorage.Limit.ACCOMPLISHMENT_CAPES, 0, capes, Set.of(1, 2, 3, 9), Set.of()));
		// Teak rack: 1 skillcape (the one freeing most slots)
		assertEquals(Set.of(1, 2, 9), TierPlanner.fits(PohStorage.Limit.ACCOMPLISHMENT_CAPES, 1, capes, Set.of(1, 2, 3, 9), Set.of()));
	}

	@Test
	public void treasureChestTakesRewardsUpToItsClueTier()
	{
		List<TierPlanner.StorageSet> rewards = List.of(
			new TierPlanner.StorageSet("Easy", Set.of(1), false, PohStorage.ClueTier.EASY.ordinal()),
			new TierPlanner.StorageSet("Medium", Set.of(2), false, PohStorage.ClueTier.MEDIUM.ordinal()),
			new TierPlanner.StorageSet("Elite", Set.of(3), false, PohStorage.ClueTier.ELITE.ordinal()));
		// Oak chest: up to easy
		assertEquals(Set.of(1), TierPlanner.fits(PohStorage.Limit.CLUE_TIER, PohStorage.ClueTier.EASY.ordinal(),
			rewards, Set.of(1, 2, 3), Set.of()));
		// Unknown items (in no set) aren't blocked
		assertEquals(Set.of(1, 2, 99), TierPlanner.fits(PohStorage.Limit.CLUE_TIER, PohStorage.ClueTier.MEDIUM.ordinal(),
			rewards, Set.of(1, 2, 3, 99), Set.of()));
	}
}

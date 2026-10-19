package com.ultimatebank.feature.storagesavings.tiers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

/**
 * Works out which banked items fit a POH storage at a given tier. Pure: no client access.
 * <ul>
 * <li>A set already in the storage (any piece stored) keeps its slot, so its banked pieces always fit (storage
 * stacks).</li>
 * <li>Remaining slots go to the sets that free the most bank slots (most banked pieces), largest first.</li>
 * <li>Cape rack: only capes of accomplishment are limited; every other cape fits.</li>
 * <li>Treasure chest: rewards fit when their clue tier is within the chest's tier.</li>
 * </ul>
 */
public final class TierPlanner
{
	private TierPlanner()
	{
	}

	@Value
	public static class StorageSet
	{
		String name;
		Set<Integer> itemIds;
		boolean accomplishment;
		/** {@link PohStorage.ClueTier} ordinal, or -1 if unknown */
		int clueTier;
	}

	/**
	 * @param capacity the tier's capacity (see {@link PohStorage.Limit}); {@link PohStorage.Tier#UNLIMITED} for
	 *                 no limit
	 * @param banked   banked items that this storage could otherwise hold
	 * @param stored   items the storage already holds (from its snapshot), empty if unknown
	 * @return the banked items that fit
	 */
	public static Set<Integer> fits(PohStorage.Limit limit, int capacity, List<StorageSet> sets, Set<Integer> banked,
		Set<Integer> stored)
	{
		if (limit == PohStorage.Limit.CLUE_TIER)
		{
			return fitsClueTier(capacity, sets, banked);
		}

		Set<Integer> fits = new HashSet<>();
		List<Candidate> candidates = new ArrayList<>();
		int used = 0;
		for (StorageSet set : sets)
		{
			Set<Integer> bankedPieces = intersect(set.getItemIds(), banked);
			boolean limited = limit == PohStorage.Limit.SETS || set.isAccomplishment();
			if (!limited)
			{
				fits.addAll(bankedPieces);
			}
			else if (!intersect(set.getItemIds(), stored).isEmpty())
			{
				used++;
				fits.addAll(bankedPieces);
			}
			else if (!bankedPieces.isEmpty())
			{
				candidates.add(new Candidate(set.getName(), bankedPieces));
			}
		}

		long free = capacity == PohStorage.Tier.UNLIMITED ? Long.MAX_VALUE : Math.max(0, capacity - used);
		candidates.stream()
			.sorted(Comparator.comparingInt((Candidate c) -> c.getPieces().size()).reversed()
				.thenComparing(Candidate::getName))
			.limit(free)
			.forEach(c -> fits.addAll(c.getPieces()));
		return fits;
	}

	private static Set<Integer> fitsClueTier(int maxTier, List<StorageSet> sets, Set<Integer> banked)
	{
		Set<Integer> tooHigh = new HashSet<>();
		for (StorageSet set : sets)
		{
			if (set.getClueTier() > maxTier)
			{
				tooHigh.addAll(set.getItemIds());
			}
		}
		Set<Integer> fits = new HashSet<>(banked);
		fits.removeAll(tooHigh);
		return fits;
	}

	private static Set<Integer> intersect(Set<Integer> a, Set<Integer> b)
	{
		Set<Integer> result = new HashSet<>(a);
		result.retainAll(b);
		return result;
	}

	@Value
	private static class Candidate
	{
		String name;
		Set<Integer> pieces;
	}
}

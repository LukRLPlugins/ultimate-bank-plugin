package com.ultimatebank.feature.storagesavings;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.Value;

/**
 * The latest storage savings results, shared by the feature (writer, client thread), the overlay
 * (client thread) and the panel (Swing thread). Results are replaced whole, never modified.
 */
@Singleton
class StorageSavingsState
{
	private volatile Results results = Results.EMPTY;

	Results get()
	{
		return results;
	}

	void set(Results results)
	{
		this.results = results;
	}

	/**
	 * @return location names by flagged item ID
	 */
	Map<Integer, List<String>> getFlagged()
	{
		return results.getFlagged();
	}

	@Value
	static class Results
	{
		static final Results EMPTY = new Results(false, 0, 0, Collections.emptyMap(), Collections.emptyList());

		/** Whether bank contents are known (seen this session, or from a saved snapshot) */
		boolean bankSeen;
		/** When the saved bank snapshot in use was taken, or 0 if the bank was seen live this session */
		long bankSnapshotAt;
		/** Flagged items that a suggested POH storage already holds copies of (shown for information) */
		int alreadyStored;
		/** Location names by flagged item ID */
		Map<Integer, List<String>> flagged;
		/** Storage locations by bank slots they would free, most first */
		List<LocationSavings> ranking;
	}

	@Value
	static class LocationSavings
	{
		String name;
		/** Whether this is a player-owned house build */
		boolean poh;
		/** Names of the flagged items it could hold, sorted */
		List<String> itemNames;
		/** IDs of the same items, in the same order */
		List<Integer> itemIds;
	}
}

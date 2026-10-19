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
		static final Results EMPTY = new Results(false, Collections.emptyMap(), Collections.emptyList());

		/** Whether the bank has been seen this session; nothing can be flagged before then */
		boolean bankSeen;
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

package com.ultimatebank.feature.sortedtabs;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The user's changes to one tab, stored as a diff over the defaults (JSON in config). Anything not mentioned here
 * follows the defaults, so new items from data updates still appear unless the user moved or removed them.
 */
@Data
class TabEdits
{
	/**
	 * The tab's sections in display order, once the user has changed the structure (renamed, added, reordered).
	 * Null means the default structure. Default sections that are neither listed here nor deleted (e.g. added by
	 * a later data update) are appended at the end.
	 */
	List<Section> sections;

	/** IDs of default sections the user deleted; their items are hidden unless moved elsewhere */
	Set<String> deletedSections = new LinkedHashSet<>();

	/** Items placed in a specific section by the user (moved or added), in the order they were placed */
	Map<Integer, String> moved = new LinkedHashMap<>();

	/** Items the user removed from this tab */
	Set<Integer> removed = new LinkedHashSet<>();

	boolean isEmpty()
	{
		return sections == null && deletedSections.isEmpty() && moved.isEmpty() && removed.isEmpty();
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	static class Section
	{
		/** A default section's name (its sort rules key) or {@code custom:<n>} for user sections */
		String id;
		String title;
		/** 1 = section, 2 = sub-section */
		int level;
	}
}

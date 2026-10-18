package com.ultimatebank.feature.sortedtabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import lombok.Value;

/**
 * Combines a tab's defaults with the user's {@link TabEdits} into the sections to display. Pure: no client access.
 */
final class TabResolver
{
	static final String CUSTOM_PREFIX = "custom:";

	private TabResolver()
	{
	}

	@Value
	static class ResolvedSection
	{
		String id;
		String title;
		/** 1 = section, 2 = sub-section */
		int level;
		/** Default section name whose sort rules apply, or null for user sections (kept in placement order) */
		String sortDomain;
		/** Every item in the section (owned or not), defaults first then user-placed */
		List<Integer> items;
	}

	/**
	 * @param defaultItems    every item in the tab by default
	 * @param defaultOrder    the tab's default section names in display order; the last one is the fallback for
	 *                        items with no known section
	 * @param defaultSectionOf an item's default section name in this tab, or null if unknown
	 * @param userEdits       the user's changes, or null for none
	 */
	static List<ResolvedSection> resolve(List<Integer> defaultItems, List<String> defaultOrder,
		IntFunction<String> defaultSectionOf, TabEdits userEdits)
	{
		TabEdits edits = userEdits == null ? new TabEdits() : userEdits;

		// Sections: the user's structure, plus any default sections they haven't seen or deleted
		List<TabEdits.Section> sections = new ArrayList<>();
		if (edits.getSections() != null)
		{
			sections.addAll(edits.getSections());
		}
		for (String name : defaultOrder)
		{
			boolean listed = sections.stream().anyMatch(s -> s.getId().equals(name));
			if (!listed && !edits.getDeletedSections().contains(name))
			{
				sections.add(new TabEdits.Section(name, shortLabel(name), 1));
			}
		}

		Map<String, List<Integer>> items = new LinkedHashMap<>();
		for (TabEdits.Section section : sections)
		{
			items.put(section.getId(), new ArrayList<>());
		}

		String fallback = defaultOrder.isEmpty() ? null : defaultOrder.get(defaultOrder.size() - 1);
		for (int itemId : defaultItems)
		{
			if (itemId <= 0 || edits.getRemoved().contains(itemId) || edits.getMoved().containsKey(itemId))
			{
				continue;
			}
			String section = defaultSectionOf.apply(itemId);
			if (section == null || !defaultOrder.contains(section))
			{
				section = fallback;
			}
			List<Integer> bucket = section == null ? null : items.get(section);
			if (bucket != null)
			{
				bucket.add(itemId);
			}
		}

		edits.getMoved().forEach((itemId, sectionId) ->
		{
			List<Integer> bucket = items.get(sectionId);
			if (bucket != null && !edits.getRemoved().contains(itemId))
			{
				bucket.add(itemId);
			}
		});

		List<ResolvedSection> resolved = new ArrayList<>();
		for (TabEdits.Section section : sections)
		{
			String id = section.getId();
			resolved.add(new ResolvedSection(id, section.getTitle(), section.getLevel(),
				id.startsWith(CUSTOM_PREFIX) ? null : id,
				Collections.unmodifiableList(items.get(id))));
		}
		return resolved;
	}

	/**
	 * Short title for a default section: cut at " &amp; " and parentheticals, at most ~22 characters on a word
	 * boundary ("Teleport tablets &amp; spell utility" becomes "Teleport tablets"). From Bank Skill Sorting.
	 */
	static String shortLabel(String section)
	{
		String s = section;
		int amp = s.indexOf(" & ");
		if (amp > 0)
		{
			s = s.substring(0, amp);
		}
		int paren = s.indexOf(" (");
		if (paren > 0)
		{
			s = s.substring(0, paren);
		}
		if (s.length() > 22)
		{
			int cut = s.lastIndexOf(' ', 22);
			if (cut > 0)
			{
				s = s.substring(0, cut);
			}
		}
		return s;
	}
}

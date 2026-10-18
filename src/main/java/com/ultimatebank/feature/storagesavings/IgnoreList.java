package com.ultimatebank.feature.storagesavings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.runelite.client.util.Text;

/**
 * Resolves the user's ignore list (comma-separated item names, item IDs or {@code *} wildcards) into item IDs.
 * Names match ignoring case and whitespace, and a name shared by several items ignores all of them.
 */
final class IgnoreList
{
	private IgnoreList()
	{
	}

	/**
	 * @param namesToIds item IDs by {@link #normalize normalized} name, for every item that can be flagged
	 */
	static Set<Integer> resolve(String csv, Map<String, Set<Integer>> namesToIds)
	{
		if (csv == null || csv.isBlank())
		{
			return Collections.emptySet();
		}

		Set<Integer> ids = new HashSet<>();
		for (String entry : Text.fromCSV(csv))
		{
			String name = normalize(entry);
			if (name.isEmpty())
			{
				continue;
			}

			if (name.chars().allMatch(Character::isDigit))
			{
				try
				{
					ids.add(Integer.parseInt(name));
				}
				catch (NumberFormatException e)
				{
					// too long to be an item ID
				}
			}
			else if (name.indexOf('*') >= 0)
			{
				Pattern pattern = wildcard(name);
				namesToIds.forEach((itemName, itemIds) ->
				{
					if (pattern.matcher(itemName).matches())
					{
						ids.addAll(itemIds);
					}
				});
			}
			else
			{
				ids.addAll(namesToIds.getOrDefault(name, Collections.emptySet()));
			}
		}
		return ids;
	}

	/**
	 * @return whether the list has an entry naming exactly this item (by name or ID), i.e. one that
	 * {@link #remove} would take out
	 */
	static boolean containsExact(String csv, String itemName, int itemId)
	{
		String name = normalize(itemName);
		String id = String.valueOf(itemId);
		return Text.fromCSV(csv == null ? "" : csv).stream()
			.map(IgnoreList::normalize)
			.anyMatch(entry -> entry.equals(name) || entry.equals(id));
	}

	static String add(String csv, String itemName)
	{
		Collection<String> entries = new ArrayList<>(Text.fromCSV(csv == null ? "" : csv));
		entries.add(itemName);
		return Text.toCSV(entries);
	}

	/**
	 * Removes entries naming exactly this item. Wildcards that match it are left alone.
	 */
	static String remove(String csv, String itemName, int itemId)
	{
		String name = normalize(itemName);
		String id = String.valueOf(itemId);
		Collection<String> entries = new ArrayList<>(Text.fromCSV(csv == null ? "" : csv));
		entries.removeIf(entry ->
		{
			String normalized = normalize(entry);
			return normalized.equals(name) || normalized.equals(id);
		});
		return Text.toCSV(entries);
	}

	static String normalize(String name)
	{
		return name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
	}

	private static Pattern wildcard(String normalized)
	{
		return Pattern.compile(Arrays.stream(normalized.split("\\*", -1))
			.map(Pattern::quote)
			.collect(Collectors.joining(".*")));
	}
}

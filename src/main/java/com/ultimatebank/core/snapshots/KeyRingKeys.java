package com.ultimatebank.core.snapshots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The keys seen on the steel key ring while its interface is open. The game shows one key at a time (with the
 * previous and next ones as buttons) plus a total, so the ring is known once every key has been paged past, or once
 * the keys last recorded plus those just seen add up to the total. Pure.
 */
final class KeyRingKeys
{
	private final Set<Integer> seen = new HashSet<>();
	private int total = -1;

	void see(int itemId)
	{
		if (itemId > 0)
		{
			seen.add(itemId);
		}
	}

	/**
	 * A new total means a key was added or removed: the keys seen so far may include one that's gone, so start over.
	 */
	void setTotal(int total)
	{
		if (total != this.total)
		{
			this.total = total;
			seen.clear();
		}
	}

	int getTotal()
	{
		return total;
	}

	int seenCount()
	{
		return seen.size();
	}

	/**
	 * @param previous the keys last recorded, or empty
	 * @return the ring's keys (one of each), or null if they aren't known yet
	 */
	Map<Integer, Integer> contents(Set<Integer> previous)
	{
		if (total < 0)
		{
			return null;
		}
		Set<Integer> keys = new HashSet<>(seen);
		if (keys.size() < total)
		{
			keys.addAll(previous);
		}
		if (keys.size() != total)
		{
			return null;
		}
		Map<Integer, Integer> contents = new HashMap<>();
		keys.forEach(key -> contents.put(key, 1));
		return contents;
	}

	/**
	 * @return the number in the interface's "Key total: N" text, or -1
	 */
	static int parseTotal(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String digits = text.replaceAll("<[^>]*>", "").replaceAll("[^0-9]", "");
		if (digits.isEmpty() || digits.length() > 3)
		{
			return -1;
		}
		return Integer.parseInt(digits);
	}
}

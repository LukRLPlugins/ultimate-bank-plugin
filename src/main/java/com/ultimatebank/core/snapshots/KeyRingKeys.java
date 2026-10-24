package com.ultimatebank.core.snapshots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The keys seen on the steel key ring while its interface is open. The game shows one key at a time (with the
 * previous and next ones as buttons) plus a total, so the ring is known once every key has been paged past, or once
 * the keys last recorded plus those just seen add up to the total. Removing a key shows the next one with a total one
 * lower, so the key that was showing is the one removed. Pure.
 */
final class KeyRingKeys
{
	private final Set<Integer> seen = new HashSet<>();
	/** Keys taken off the ring this login, until one is added again */
	private final Set<Integer> removed = new HashSet<>();
	private int total = -1;
	/** The key the ring is showing, or -1 */
	private int shown = -1;

	void see(int itemId)
	{
		if (itemId > 0)
		{
			seen.add(itemId);
		}
	}

	/**
	 * The ring now shows this key (call after {@link #setTotal}).
	 */
	void show(int itemId)
	{
		see(itemId);
		shown = itemId;
	}

	/**
	 * A new total means a key was added or removed: the keys seen so far may include one that's gone, so start over.
	 * One fewer, while a key was showing, means that key was removed. Keys seen before the first total are kept.
	 */
	void setTotal(int total)
	{
		if (total == this.total)
		{
			return;
		}
		if (this.total >= 0)
		{
			seen.clear();
			if (total == this.total - 1 && shown > 0)
			{
				removed.add(shown);
			}
			else if (total > this.total)
			{
				removed.clear();
			}
		}
		this.total = total;
		shown = -1;
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
			keys.removeAll(removed);
			keys.addAll(seen);
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
	 * @return the key named by "You add the dusty key to your key ring." (lower case), or null
	 */
	static String addedKey(String message)
	{
		return keyIn(message, "you add the ", " to your key ring.");
	}

	/**
	 * @return the key named by "You remove the dusty key from your key ring." (lower case), or null
	 */
	static String removedKey(String message)
	{
		return keyIn(message, "you remove the ", " from your key ring.");
	}

	private static String keyIn(String message, String start, String end)
	{
		if (message == null)
		{
			return null;
		}
		String text = message.replaceAll("<[^>]*>", "").trim().toLowerCase(Locale.ROOT);
		if (!text.startsWith(start) || !text.endsWith(end) || text.length() <= start.length() + end.length())
		{
			return null;
		}
		return text.substring(start.length(), text.length() - end.length());
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

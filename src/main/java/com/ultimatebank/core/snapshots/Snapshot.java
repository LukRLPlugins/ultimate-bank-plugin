package com.ultimatebank.core.snapshots;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Item quantities in one location at one moment. Stored as JSON, so fields stay simple.
 */
@Getter
@EqualsAndHashCode
public final class Snapshot
{
	/** Quantity by item ID, as the game reports them (placeholders excluded) */
	private final Map<Integer, Integer> quantities;
	/** When it was taken, epoch millis */
	private final long takenAt;

	public Snapshot(Map<Integer, Integer> quantities, long takenAt)
	{
		this.quantities = Collections.unmodifiableMap(new HashMap<>(quantities));
		this.takenAt = takenAt;
	}

	/**
	 * @return e.g. "5 minutes ago", "3 hours ago", "2 days ago"
	 */
	public static String ago(long epochMillis)
	{
		long minutes = Math.max(0, (System.currentTimeMillis() - epochMillis) / 60_000);
		if (minutes < 60)
		{
			return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
		}
		long hours = minutes / 60;
		if (hours < 48)
		{
			return hours + (hours == 1 ? " hour ago" : " hours ago");
		}
		return hours / 24 + " days ago";
	}

	public boolean sameContents(Snapshot other)
	{
		return other != null && quantities.equals(other.quantities);
	}
}

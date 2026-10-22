package com.ultimatebank.feature.banksearch;

import com.ultimatebank.core.snapshots.SnapshotLocation;
import lombok.Value;

/**
 * One item in one location, as shown in the Ultimate Bank Search panel. In a visit, quantity is the change (negative =
 * taken out).
 */
@Value
class StuffRow
{
	int itemId;
	String name;
	int quantity;
	/** GE price, or high alch value for untradeables */
	long unitPrice;
	SnapshotLocation location;

	long totalValue()
	{
		return unitPrice * quantity;
	}
}

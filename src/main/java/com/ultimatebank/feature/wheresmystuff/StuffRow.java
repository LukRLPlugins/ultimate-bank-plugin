package com.ultimatebank.feature.wheresmystuff;

import com.ultimatebank.core.snapshots.SnapshotLocation;
import lombok.Value;

/**
 * One item in one location, as shown in the Where's my stuff panel.
 */
@Value
class StuffRow
{
	int itemId;
	String name;
	/** 0 when the item has left this location since the previous snapshot */
	int quantity;
	/** GE price, or high alch value for untradeables */
	long unitPrice;
	SnapshotLocation location;
	/** Change since the previous snapshot of this location (0 = unchanged) */
	int change;

	long totalValue()
	{
		return unitPrice * quantity;
	}
}

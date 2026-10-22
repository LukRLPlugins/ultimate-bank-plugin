package com.ultimatebank.feature.banksearch;

import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.List;
import lombok.Value;

/**
 * One visit as shown in the panel's history: what was taken out or put in, with names and prices.
 */
@Value
class StuffVisit
{
	SnapshotLocation location;
	long takenAt;
	/** Quantity = the change (negative = taken out) */
	List<StuffRow> rows;

	long netValue()
	{
		return rows.stream().mapToLong(StuffRow::totalValue).sum();
	}
}

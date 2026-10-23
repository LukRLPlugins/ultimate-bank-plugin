package com.ultimatebank.feature.banksearch;

import net.runelite.api.gameval.ItemID;

/**
 * Which price the Ultimate Bank Search panel values items at.
 */
public enum ValueType
{
	/** Grand Exchange price; high alch value for untradeables */
	GE("GE price"),
	HIGH_ALCH("High alch");

	private final String label;

	ValueType(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}

	/**
	 * @param gePrice the item's GE price (0 if untradeable)
	 * @param haPrice the item's high alch value
	 * @return one item's value
	 */
	long unitPrice(int itemId, long gePrice, long haPrice)
	{
		// Coins and platinum tokens alch for nothing, but are money
		if (itemId == ItemID.COINS)
		{
			return 1;
		}
		if (itemId == ItemID.PLATINUM)
		{
			return 1_000;
		}
		if (this == HIGH_ALCH)
		{
			return haPrice;
		}
		return gePrice > 0 ? gePrice : haPrice;
	}
}

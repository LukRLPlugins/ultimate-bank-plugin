package com.ultimatebank.core.snapshots;

import java.util.Locale;
import lombok.Getter;

/**
 * Places whose contents are snapshotted: the bank and each player-owned house costume room storage.
 */
@Getter
public enum SnapshotLocation
{
	BANK("Bank", false),
	MAGIC_WARDROBE("POH magic wardrobe", true),
	TOY_BOX("POH toy box", true),
	CAPE_RACK("POH cape rack", true),
	TREASURE_CHEST("POH treasure chest", true),
	ARMOUR_CASE("POH armour case", true),
	FANCY_DRESS_BOX("POH fancy dress box", true),
	BOOKCASE("POH bookcase", true);

	private final String displayName;
	private final boolean poh;

	SnapshotLocation(String displayName, boolean poh)
	{
		this.displayName = displayName;
		this.poh = poh;
	}

	/**
	 * Works out which POH storage an interface is showing from its text, e.g. a "Toy box" title.
	 * From Where's My Stuff by Wozak-1.
	 *
	 * @return the storage, or null if the text names none
	 */
	public static SnapshotLocation fromInterfaceText(String text)
	{
		if (text == null)
		{
			return null;
		}
		String t = text.toLowerCase(Locale.ROOT);
		if (t.contains("magic wardrobe"))
		{
			return MAGIC_WARDROBE;
		}
		if (t.contains("toy box"))
		{
			return TOY_BOX;
		}
		if (t.contains("cape rack"))
		{
			return CAPE_RACK;
		}
		if (t.contains("treasure chest"))
		{
			return TREASURE_CHEST;
		}
		if (t.contains("armour case") || t.contains("armor case"))
		{
			return ARMOUR_CASE;
		}
		if (t.contains("fancy dress box") || t.contains("costume box"))
		{
			return FANCY_DRESS_BOX;
		}
		if (t.contains("bookcase") || t.contains("bookshelf"))
		{
			return BOOKCASE;
		}
		return null;
	}
}

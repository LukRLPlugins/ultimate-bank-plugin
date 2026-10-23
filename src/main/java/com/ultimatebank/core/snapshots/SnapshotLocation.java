package com.ultimatebank.core.snapshots;

import java.util.Locale;
import lombok.Getter;

/**
 * Places whose contents are snapshotted: the bank, each player-owned house costume room storage, and the storages
 * read from an item container ({@link ContainerStorage}). New values go at the end: the order is the panel's order.
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
	BOOKCASE("POH bookcase", true),
	// Read from an item container while open (ContainerStorage)
	PET_HOUSE("POH pet house", true),
	SEED_VAULT("Seed vault", false),
	TACKLE_BOX("Tackle box", false),
	HUNTSMANS_KIT("Huntsman's kit", false),
	FORESTRY_KIT("Forestry kit", false);

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

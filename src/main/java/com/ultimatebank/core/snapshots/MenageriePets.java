package com.ultimatebank.core.snapshots;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Pets living in the house menagerie, decoded as the game's [proc,poh_menagerie_drawlist] does: one bit per pet over
 * three varps, the pet's item from game enum 985. Pet items kept there are in its item container instead. Pure.
 */
final class MenageriePets
{
	/** Game enum mapping a pet's bit to its item (enum_985 in the game's scripts) */
	static final int PETS_ENUM = 985;
	/** Bits 0-31 are in the first varp, 32-62 in the second and 63-93 in the third */
	private static final int SECOND_VARP_START = 32;
	private static final int THIRD_VARP_START = 63;
	private static final int END = 94;

	private MenageriePets()
	{
	}

	/**
	 * @param varps   the three varps, in order
	 * @param petItem a pet bit's item, or a value below 1 if there's none
	 * @return the item of each pet living there
	 */
	static Set<Integer> read(int[] varps, IntUnaryOperator petItem)
	{
		Set<Integer> pets = new HashSet<>();
		for (int bit = 0; bit < END; bit++)
		{
			int varp;
			int shift;
			if (bit < SECOND_VARP_START)
			{
				varp = varps[0];
				shift = bit;
			}
			else if (bit < THIRD_VARP_START)
			{
				varp = varps[1];
				shift = bit - SECOND_VARP_START;
			}
			else
			{
				varp = varps[2];
				shift = bit - THIRD_VARP_START;
			}
			if ((varp >>> shift & 1) == 1)
			{
				int item = petItem.applyAsInt(bit);
				if (item > 0)
				{
					pets.add(item);
				}
			}
		}
		return pets;
	}
}

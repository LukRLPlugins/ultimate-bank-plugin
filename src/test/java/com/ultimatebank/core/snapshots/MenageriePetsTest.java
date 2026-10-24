package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Set;
import org.junit.Test;

public class MenageriePetsTest
{
	@Test
	public void eachVarpHoldsItsRangeOfBits()
	{
		// Bit 0 and 31 in the first varp, 32 in the second (its bit 0), 63 and 70 in the third (bits 0 and 7)
		int[] varps = {1 | 1 << 31, 1, 1 | 1 << 7};
		Set<Integer> pets = MenageriePets.read(varps, bit -> 1000 + bit);
		assertEquals(Set.of(1000, 1031, 1032, 1063, 1070), pets);
	}

	@Test
	public void bitsWithoutAPetAreSkipped()
	{
		assertTrue(MenageriePets.read(new int[]{0b11, 0, 0}, bit -> -1).isEmpty());
		assertTrue(MenageriePets.read(new int[]{0, 0, 0}, bit -> 5).isEmpty());
	}
}

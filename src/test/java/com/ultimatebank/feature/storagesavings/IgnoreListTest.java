package com.ultimatebank.feature.storagesavings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class IgnoreListTest
{
	private static final Map<String, Set<Integer>> NAMES = Map.of(
		IgnoreList.normalize("Mystic robe top"), Set.of(4091),
		IgnoreList.normalize("Mystic robe top (dark)"), Set.of(4101),
		IgnoreList.normalize("Watering can(7)"), Set.of(5333),
		// Two items sharing a name, e.g. a quest and non-quest variant
		IgnoreList.normalize("Bullseye lantern"), Set.of(4550, 9065)
	);

	@Test
	public void emptyListIgnoresNothing()
	{
		assertEquals(Set.of(), IgnoreList.resolve("", NAMES));
		assertEquals(Set.of(), IgnoreList.resolve(null, NAMES));
	}

	@Test
	public void namesMatchIgnoringCaseAndSpaces()
	{
		assertEquals(Set.of(4091), IgnoreList.resolve("mySTic   robetop", NAMES));
	}

	@Test
	public void sharedNameIgnoresEveryItem()
	{
		assertEquals(Set.of(4550, 9065), IgnoreList.resolve("Bullseye lantern", NAMES));
	}

	@Test
	public void numbersAreItemIds()
	{
		assertEquals(Set.of(1234, 4091), IgnoreList.resolve("1234, Mystic robe top", NAMES));
	}

	@Test
	public void hugeNumberIsSkipped()
	{
		assertEquals(Set.of(), IgnoreList.resolve("99999999999999", NAMES));
	}

	@Test
	public void wildcardsMatchAndTreatBracketsLiterally()
	{
		assertEquals(Set.of(4091, 4101), IgnoreList.resolve("*mystic robe top*", NAMES));
		assertEquals(Set.of(5333), IgnoreList.resolve("watering can(*)", NAMES));
	}

	@Test
	public void unknownNameIsSkipped()
	{
		assertEquals(Set.of(), IgnoreList.resolve("Not an item", NAMES));
	}

	@Test
	public void addAndRemoveExactEntries()
	{
		String list = IgnoreList.add("*cape*", "Mystic robe top");
		assertTrue(IgnoreList.containsExact(list, "Mystic robe top", 4091));

		list = IgnoreList.remove(list, "Mystic robe top", 4091);
		assertFalse(IgnoreList.containsExact(list, "Mystic robe top", 4091));
		assertEquals("Wildcards are left alone", "*cape*", list);
	}

	@Test
	public void removeMatchesById()
	{
		assertEquals("", IgnoreList.remove("4091", "Mystic robe top", 4091));
	}
}

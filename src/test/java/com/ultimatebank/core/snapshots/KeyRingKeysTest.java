package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class KeyRingKeysTest
{
	@Test
	public void knownOnceEveryKeyIsSeen()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.setTotal(3);
		ring.see(1);
		ring.see(2);
		assertNull(ring.contents(Set.of()));
		ring.see(3);
		ring.see(1);
		assertEquals(Map.of(1, 1, 2, 1, 3, 1), ring.contents(Set.of()));
	}

	@Test
	public void lastRecordedKeysFillTheGaps()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.setTotal(3);
		ring.see(4);
		// A key was added to a ring last seen holding 1 and 2
		assertEquals(Map.of(1, 1, 2, 1, 4, 1), ring.contents(Set.of(1, 2)));
	}

	@Test
	public void aRemovedKeyWaitsForAFullPage()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.setTotal(2);
		ring.see(1);
		// Last seen with 1, 2 and 3: one is gone but which isn't known yet
		assertNull(ring.contents(Set.of(1, 2, 3)));
		ring.see(3);
		assertEquals(Map.of(1, 1, 3, 1), ring.contents(Set.of(1, 2, 3)));
	}

	@Test
	public void removingTheShownKeyIsKnownAtOnce()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.setTotal(3);
		ring.show(2);
		// Remove: the ring shows the next key with one fewer
		ring.setTotal(2);
		ring.show(3);
		assertEquals(Map.of(1, 1, 3, 1), ring.contents(Set.of(1, 2, 3)));
	}

	@Test
	public void addingAKeyBackForgetsItsRemoval()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.setTotal(2);
		ring.show(2);
		ring.setTotal(1);
		ring.setTotal(2);
		ring.show(2);
		assertEquals(Map.of(1, 1, 2, 1), ring.contents(Set.of(1)));
	}

	@Test
	public void aNewTotalStartsOver()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.setTotal(2);
		ring.see(1);
		ring.see(2);
		ring.setTotal(1);
		assertEquals(0, ring.seenCount());
		ring.see(2);
		assertEquals(Map.of(2, 1), ring.contents(Set.of()));
	}

	@Test
	public void keysSeenBeforeTheTotalAreKept()
	{
		KeyRingKeys ring = new KeyRingKeys();
		ring.see(1);
		ring.setTotal(1);
		assertEquals(Map.of(1, 1), ring.contents(Set.of()));
	}

	@Test
	public void emptyRingAndUnknownTotal()
	{
		KeyRingKeys ring = new KeyRingKeys();
		assertNull(ring.contents(Set.of()));
		ring.setTotal(0);
		assertEquals(Map.of(), ring.contents(Set.of()));
	}

	@Test
	public void parsesTheTotal()
	{
		assertEquals(12, KeyRingKeys.parseTotal("Key total: 12"));
		assertEquals(3, KeyRingKeys.parseTotal("<col=ff9040>Key total:</col> 3"));
		assertEquals(-1, KeyRingKeys.parseTotal("Key total:"));
		assertEquals(-1, KeyRingKeys.parseTotal(null));
	}
}

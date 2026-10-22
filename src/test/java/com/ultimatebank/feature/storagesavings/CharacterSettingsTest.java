package com.ultimatebank.feature.storagesavings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.HashSet;
import org.junit.Test;

public class CharacterSettingsTest
{
	@Test
	public void defaultsRoundTrip()
	{
		assertEquals(CharacterSettings.DEFAULT, CharacterSettings.toStored(null));
		assertNull(CharacterSettings.fromStored(CharacterSettings.toStored(null)));
	}

	@Test
	public void valuesRoundTrip()
	{
		assertEquals("TEAK", CharacterSettings.fromStored(CharacterSettings.toStored("TEAK")));
		assertEquals("false", CharacterSettings.fromStored(CharacterSettings.toStored("false")));
	}

	@Test
	public void keysAreUniqueStorageSavingsKeys()
	{
		assertEquals(CharacterSettings.KEYS.size(), new HashSet<>(CharacterSettings.KEYS).size());
		CharacterSettings.KEYS.forEach(key -> assertEquals(key, true, key.startsWith("storageSavings")));
	}
}

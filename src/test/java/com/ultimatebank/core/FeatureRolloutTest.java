package com.ultimatebank.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class FeatureRolloutTest
{
	private static final List<String> TOGGLES = List.of("newFeatureEnabled");

	@Test
	public void newInstallKeepsNewFeaturesOn()
	{
		assertTrue(FeatureRollout.toDisable(null, false, TOGGLES, Set.of()).isEmpty());
	}

	@Test
	public void updateFromBeforeTheMarkerTurnsThemOff()
	{
		assertEquals(TOGGLES, FeatureRollout.toDisable(null, true, TOGGLES, Set.of()));
	}

	@Test
	public void laterUpdateTurnsOnlyUnseenOnesOff()
	{
		List<String> toggles = List.of("oldFeatureEnabled", "newFeatureEnabled");
		assertEquals(List.of("newFeatureEnabled"), FeatureRollout.toDisable("oldFeatureEnabled", true, toggles, Set.of()));
	}

	@Test
	public void handledTogglesAreLeftAlone()
	{
		assertTrue(FeatureRollout.toDisable("newFeatureEnabled", true, TOGGLES, Set.of()).isEmpty());
	}

	@Test
	public void theUsersChoiceIsKept()
	{
		assertTrue(FeatureRollout.toDisable(null, true, TOGGLES, Set.of("newFeatureEnabled")).isEmpty());
	}
}

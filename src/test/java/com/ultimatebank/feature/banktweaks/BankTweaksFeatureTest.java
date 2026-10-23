package com.ultimatebank.feature.banktweaks;

import static org.junit.Assert.assertEquals;
import com.ultimatebank.feature.banktweaks.BankTweaksFeature.Option;
import java.util.List;
import org.junit.Test;

public class BankTweaksFeatureTest
{
	private static final Option CANCEL = new Option("Cancel", 0, 0);

	@Test
	public void swapsViewTagTabsOntoLeftClick()
	{
		List<Option> menu = List.of(CANCEL,
			new Option("View tag tabs", 11, 786_440),
			new Option("Import tag tab", 11, 786_440),
			new Option("New tag tab", 11, 786_440));
		assertEquals(1, BankTweaksFeature.leftClickSwap(menu));
	}

	@Test
	public void leavesOtherLeftClicksAlone()
	{
		List<Option> menu = List.of(CANCEL, new Option("View tag tabs", 11, 786_440), new Option("Withdraw-1", 3, 786_444));
		assertEquals(-1, BankTweaksFeature.leftClickSwap(menu));
	}

	@Test
	public void onlySwapsWithinTheSameButton()
	{
		List<Option> menu = List.of(CANCEL, new Option("View tag tabs", 12, 786_440), new Option("New tag tab", 11, 786_440));
		assertEquals(-1, BankTweaksFeature.leftClickSwap(menu));
	}

	@Test
	public void alreadySwappedOrMissingDoesNothing()
	{
		assertEquals(-1, BankTweaksFeature.leftClickSwap(List.of(CANCEL, new Option("View tag tabs", 11, 786_440))));
		assertEquals(-1, BankTweaksFeature.leftClickSwap(List.of(CANCEL, new Option("New tag tab", 11, 786_440))));
	}
}

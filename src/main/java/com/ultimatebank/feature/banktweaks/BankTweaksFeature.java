package com.ultimatebank.feature.banktweaks;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.BankFeature;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Small changes to how the bank handles clicks. Each only reorders a menu's existing options (like core's Menu Entry
 * Swapper), so nothing new is sent to the server.
 */
@Singleton
public class BankTweaksFeature implements BankFeature
{
	/** Core Bank Tags' "+" button below the tag tabs (TabInterface) */
	static final String NEW_TAG_TAB = "New tag tab";
	static final String VIEW_TAG_TABS = "View tag tabs";

	@Inject
	private Client client;

	@Inject
	private UltimateBankConfig config;

	@Override
	public boolean isEnabled(UltimateBankConfig config)
	{
		return config.bankTweaksEnabled();
	}

	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (client.isMenuOpen() || !config.bankTweaksViewTagTabs())
		{
			return;
		}

		Menu menu = client.getMenu();
		MenuEntry[] entries = menu.getMenuEntries();
		int top = entries.length - 1;
		// Cheap check first: this runs every frame
		if (top < 1 || !NEW_TAG_TAB.equals(Text.removeTags(entries[top].getOption())))
		{
			return;
		}

		List<Option> options = new ArrayList<>(entries.length);
		for (MenuEntry entry : entries)
		{
			options.add(new Option(Text.removeTags(entry.getOption()), entry.getParam0(), entry.getParam1()));
		}
		int swap = leftClickSwap(options);
		if (swap != -1)
		{
			MenuEntry leftClick = entries[top];
			entries[top] = entries[swap];
			entries[swap] = leftClick;
			menu.setMenuEntries(entries);
		}
	}

	/** A menu entry's option text (tags removed) and the widget it's on */
	@Value
	static class Option
	{
		String text;
		int param0;
		int param1;
	}

	/**
	 * @param options menu entries in client order (the last one is the left-click)
	 * @return the index of the "View tag tabs" entry to swap with a left-click "New tag tab", or -1
	 */
	static int leftClickSwap(List<Option> options)
	{
		int top = options.size() - 1;
		if (top < 1 || !NEW_TAG_TAB.equals(options.get(top).getText()))
		{
			return -1;
		}
		Option button = options.get(top);
		for (int i = top - 1; i >= 0; i--)
		{
			Option option = options.get(i);
			if (option.getParam0() == button.getParam0() && option.getParam1() == button.getParam1()
				&& VIEW_TAG_TABS.equals(option.getText()))
			{
				return i;
			}
		}
		return -1;
	}
}

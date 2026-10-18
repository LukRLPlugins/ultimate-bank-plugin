package com.ultimatebank.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.Value;

/**
 * Lets one feature publish a sectioned bank tab (e.g. Storage savings' storable items by location) for another
 * feature to display (Sorted tabs shows them as "(ub)" tabs), without the features depending on each other.
 * All methods must be called on the client thread.
 */
@Singleton
public class ExtraTabs
{
	@Value
	public static class Section
	{
		String title;
		List<Integer> items;
	}

	@Value
	public static class Tab
	{
		/** Stable id, e.g. "storable"; the tab is shown as "{id} (ub)" */
		String id;
		/** Item shown as the tab icon */
		int icon;
		/** Sections in display order */
		List<Section> sections;
	}

	private final Map<String, Tab> tabs = new LinkedHashMap<>();
	private final List<Runnable> listeners = new ArrayList<>();
	private boolean displayed;

	/**
	 * Publishes or replaces a tab.
	 */
	public void publish(Tab tab)
	{
		if (!tab.equals(tabs.put(tab.getId(), tab)))
		{
			notifyListeners();
		}
	}

	public void remove(String id)
	{
		if (tabs.remove(id) != null)
		{
			notifyListeners();
		}
	}

	public Collection<Tab> getTabs()
	{
		return Collections.unmodifiableCollection(tabs.values());
	}

	/**
	 * Set by the feature that displays published tabs, so publishers can hide their own fallback display.
	 */
	public void setDisplayed(boolean displayed)
	{
		if (this.displayed != displayed)
		{
			this.displayed = displayed;
			notifyListeners();
		}
	}

	public boolean isDisplayed()
	{
		return displayed;
	}

	/**
	 * Called after any tab is published or removed, or the displayed state changes. Remove on shutdown.
	 */
	public void addListener(Runnable listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Runnable listener)
	{
		listeners.remove(listener);
	}

	private void notifyListeners()
	{
		for (Runnable listener : new ArrayList<>(listeners))
		{
			listener.run();
		}
	}
}

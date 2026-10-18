package com.ultimatebank.feature.sortedtabs;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import net.runelite.client.util.Text;

/**
 * Section titles of the most recently built layout of each sorted tab, keyed by standardized bank tag name.
 * Written by the feature, read by the overlay.
 */
@Singleton
class SortedTabsState
{
	private final Map<String, Map<Integer, SortedLayoutBuilder.Header>> headers = new ConcurrentHashMap<>();

	Map<Integer, SortedLayoutBuilder.Header> headersFor(String bankTag)
	{
		return bankTag == null ? Collections.emptyMap() : headers.getOrDefault(Text.standardize(bankTag), Collections.emptyMap());
	}

	void setHeaders(String bankTag, Map<Integer, SortedLayoutBuilder.Header> tabHeaders)
	{
		headers.put(Text.standardize(bankTag), tabHeaders);
	}

	void clear()
	{
		headers.clear();
	}
}

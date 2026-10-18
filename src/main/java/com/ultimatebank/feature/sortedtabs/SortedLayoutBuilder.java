package com.ultimatebank.feature.sortedtabs;

import com.ultimatebank.feature.sortedtabs.data.ItemMeta;
import com.ultimatebank.feature.sortedtabs.data.SortData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.banktags.tabs.Layout;

/**
 * Turns a tab's resolved sections into a core bank tag {@link Layout} of the items the player owns: each section
 * starts on a new row, preceded by a blank title row (drawn by {@link SortedTabsOverlay}). Variant-aware: degraded or
 * charged variants take their base item's place. Placement logic adapted from Bank Skill Sorting (Frailrain).
 * Must be called on the client thread.
 */
@Singleton
class SortedLayoutBuilder
{
	static final int ROW = 8;
	static final String LOWER_TIER = "Lower tier";

	private final ItemManager itemManager;
	private final SectionSorter sorter;

	@Inject
	SortedLayoutBuilder(ItemManager itemManager, SectionSorter sorter)
	{
		this.itemManager = itemManager;
		this.sorter = sorter;
	}

	@Value
	static class Header
	{
		String title;
		/** 1 = section, 2 = sub-section */
		int level;
	}

	@Value
	static class Built
	{
		Layout layout;
		/** Titles by the (blank) layout row they're drawn on */
		Map<Integer, Header> headers;
	}

	/**
	 * @param ownedByBase variation base ID to every ID of that family in the bank (base first)
	 * @param titles      reserve a title row above each section
	 * @param lowerTier   split combat tabs' default sections into loadout and a "Lower tier" group at the bottom
	 */
	Built build(String bankTag, String tabId, List<TabResolver.ResolvedSection> sections,
		Map<Integer, List<Integer>> ownedByBase, boolean titles, boolean lowerTier)
	{
		Layout layout = new Layout(bankTag);
		Map<Integer, Header> headers = new LinkedHashMap<>();
		boolean twoZone = lowerTier && SortData.TWO_ZONE_TABS.contains(tabId);
		Map<Integer, ItemMeta> meta = SortData.itemMeta();

		// Which of each section's items are owned, and which bank IDs show for them. A variation family is placed
		// once per tab, by the first section that claims it.
		Set<Integer> seen = new HashSet<>();
		Map<Integer, List<Integer>> display = new HashMap<>();
		List<Block> blocks = new ArrayList<>();
		for (TabResolver.ResolvedSection section : sections)
		{
			List<Integer> owned = new ArrayList<>();
			for (int itemId : section.getItems())
			{
				List<Integer> ids = ownedIds(itemId, ownedByBase, seen);
				if (!ids.isEmpty())
				{
					int canonical = itemManager.canonicalize(itemId);
					owned.add(canonical);
					display.put(canonical, ids);
				}
			}

			List<Integer> zone1 = new ArrayList<>();
			List<Integer> zone2 = new ArrayList<>();
			String domain = section.getSortDomain();
			if (domain != null && !owned.isEmpty())
			{
				sorter.sort(owned, domain, tabId);
				if (twoZone)
				{
					sorter.partition(owned, domain, tabId, zone1, zone2);
				}
				else
				{
					zone1.addAll(owned);
				}
			}
			else
			{
				zone1.addAll(owned);
			}
			blocks.add(new Block(section, zone1, zone2));
		}

		Placer placer = new Placer(layout, display, meta, tabId);
		Header parent = null;
		boolean parentShown = false;
		for (Block block : blocks)
		{
			Header header = new Header(block.section.getTitle(), block.section.getLevel());
			if (header.level == 1)
			{
				parent = header;
				parentShown = false;
			}
			if (block.zone1.isEmpty())
			{
				continue;
			}

			placer.newRow();
			if (titles)
			{
				if (header.level == 2 && parent != null && !parentShown)
				{
					headers.put(placer.titleRow(), parent);
				}
				headers.put(placer.titleRow(), header);
				parentShown = true;
			}
			placer.place(block.zone1, block.section.getSortDomain());
		}

		boolean lowerShown = false;
		for (Block block : blocks)
		{
			if (block.zone2.isEmpty())
			{
				continue;
			}
			placer.newRow();
			if (titles && !lowerShown)
			{
				headers.put(placer.titleRow(), new Header(LOWER_TIER, 1));
			}
			lowerShown = true;
			placer.place(block.zone2, block.section.getSortDomain());
		}

		return new Built(layout, Collections.unmodifiableMap(headers));
	}

	/**
	 * The bank IDs to show for a tab item, or empty if not owned. An item naming a variation base takes every owned
	 * family member (degraded Barrows etc.); an item naming a specific variant only matches that exact variant, so
	 * e.g. a holiday replica doesn't pull in the player's real item. From Bank Skill Sorting.
	 */
	private List<Integer> ownedIds(int itemId, Map<Integer, List<Integer>> ownedByBase, Set<Integer> seen)
	{
		if (itemId <= 0)
		{
			return Collections.emptyList();
		}
		int canonical = itemManager.canonicalize(itemId);
		int base = ItemVariationMapping.map(canonical);
		List<Integer> family = ownedByBase.get(base);
		if (family == null || family.isEmpty())
		{
			return Collections.emptyList();
		}

		List<Integer> candidates;
		if (canonical == base)
		{
			candidates = family;
		}
		else if (family.contains(canonical))
		{
			candidates = Collections.singletonList(canonical);
		}
		else
		{
			return Collections.emptyList();
		}

		List<Integer> fresh = new ArrayList<>();
		for (int id : candidates)
		{
			if (seen.add(id))
			{
				fresh.add(id);
			}
		}
		return fresh;
	}

	@Value
	private static class Block
	{
		TabResolver.ResolvedSection section;
		List<Integer> zone1;
		List<Integer> zone2;
	}

	/**
	 * Writes items into the layout row by row.
	 */
	private static final class Placer
	{
		private final Layout layout;
		private final Map<Integer, List<Integer>> display;
		private final Map<Integer, ItemMeta> meta;
		private final String tabId;
		private int pos;

		Placer(Layout layout, Map<Integer, List<Integer>> display, Map<Integer, ItemMeta> meta, String tabId)
		{
			this.layout = layout;
			this.display = display;
			this.meta = meta;
			this.tabId = tabId;
		}

		void newRow()
		{
			if (pos % ROW != 0)
			{
				pos = (pos / ROW + 1) * ROW;
			}
		}

		/**
		 * Reserves a blank row for a title.
		 *
		 * @return the reserved row
		 */
		int titleRow()
		{
			int row = pos / ROW;
			pos += ROW;
			return row;
		}

		void place(List<Integer> items, String sortDomain)
		{
			// Cosmetics sets and crafting gem families each get their own row
			boolean breakOnSet = sortDomain != null
				&& ("cosmetics".equals(tabId) || ("crafting".equals(tabId) && "Gems".equals(sortDomain)));
			String lastSet = null;
			for (int itemId : items)
			{
				if (breakOnSet)
				{
					ItemMeta m = meta.get(itemId);
					String set = m != null ? m.setName : null;
					if (lastSet != null && !lastSet.equals(set))
					{
						newRow();
					}
					lastSet = set;
				}
				for (int id : display.getOrDefault(itemId, Collections.singletonList(itemId)))
				{
					layout.setItemAtPos(id, pos++);
				}
			}
		}
	}
}

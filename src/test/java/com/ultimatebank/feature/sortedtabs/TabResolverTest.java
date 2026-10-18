package com.ultimatebank.feature.sortedtabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.ultimatebank.feature.sortedtabs.data.DefaultTabs;
import com.ultimatebank.feature.sortedtabs.data.SortData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

public class TabResolverTest
{
	private static final List<String> ORDER = List.of("Weapons", "Food", "Training & utility");
	private static final Map<Integer, String> SECTION_OF = Map.of(1, "Weapons", 2, "Weapons", 3, "Food");
	private static final List<Integer> ITEMS = List.of(1, 2, 3, 4);

	private static List<TabResolver.ResolvedSection> resolve(TabEdits edits)
	{
		return TabResolver.resolve(ITEMS, ORDER, SECTION_OF::get, edits);
	}

	private static TabResolver.ResolvedSection section(List<TabResolver.ResolvedSection> sections, String id)
	{
		return sections.stream().filter(s -> s.getId().equals(id)).findFirst().orElseThrow();
	}

	@Test
	public void defaultsPlaceItemsAndUseFallback()
	{
		List<TabResolver.ResolvedSection> sections = resolve(null);
		assertEquals(List.of("Weapons", "Food", "Training & utility"),
			sections.stream().map(TabResolver.ResolvedSection::getId).collect(Collectors.toList()));
		assertEquals(List.of(1, 2), section(sections, "Weapons").getItems());
		assertEquals("Unknown section goes to the last one", List.of(4), section(sections, "Training & utility").getItems());
		assertEquals("Training", section(sections, "Training & utility").getTitle());
		assertEquals("Weapons", section(sections, "Weapons").getSortDomain());
	}

	@Test
	public void removedAndMovedItems()
	{
		TabEdits edits = new TabEdits();
		edits.getRemoved().add(2);
		edits.getMoved().put(1, "Food");
		edits.getMoved().put(99, "Food");

		List<TabResolver.ResolvedSection> sections = resolve(edits);
		assertEquals(List.of(), section(sections, "Weapons").getItems());
		assertEquals("Moved and added items follow the defaults", List.of(3, 1, 99), section(sections, "Food").getItems());
	}

	@Test
	public void customStructureWithSubSections()
	{
		TabEdits edits = new TabEdits();
		edits.setSections(new ArrayList<>(List.of(
			new TabEdits.Section("Food", "Grub", 1),
			new TabEdits.Section("custom:1", "Sharks", 2),
			new TabEdits.Section("Weapons", "Weapons", 1))));
		edits.getMoved().put(3, "custom:1");

		List<TabResolver.ResolvedSection> sections = resolve(edits);
		assertEquals(List.of("Food", "custom:1", "Weapons", "Training & utility"),
			sections.stream().map(TabResolver.ResolvedSection::getId).collect(Collectors.toList()));
		assertEquals("Grub", sections.get(0).getTitle());
		assertEquals(2, sections.get(1).getLevel());
		assertNull("User sections keep placement order", sections.get(1).getSortDomain());
		assertEquals(List.of(3), sections.get(1).getItems());
	}

	@Test
	public void deletedSectionHidesItsItemsUnlessMoved()
	{
		TabEdits edits = new TabEdits();
		edits.getDeletedSections().add("Weapons");
		edits.getMoved().put(2, "Food");

		List<TabResolver.ResolvedSection> sections = resolve(edits);
		assertFalse(sections.stream().anyMatch(s -> s.getId().equals("Weapons")));
		assertEquals(List.of(3, 2), section(sections, "Food").getItems());
	}

	@Test
	public void moveToUnknownSectionIsIgnored()
	{
		TabEdits edits = new TabEdits();
		edits.getMoved().put(1, "custom:gone");
		List<TabResolver.ResolvedSection> sections = resolve(edits);
		assertTrue(sections.stream().noneMatch(s -> s.getItems().contains(1)));
	}

	@Test
	public void shortLabels()
	{
		assertEquals("Teleport tablets", TabResolver.shortLabel("Teleport tablets & spell utility"));
		assertEquals("Shields", TabResolver.shortLabel("Shields (all)"));
	}

	@Test
	public void bundledDataIsComplete()
	{
		// Test-only Gson; plugin code must use the injected one
		Gson gson = new Gson();
		SortData.initItemMeta(gson);
		List<DefaultTabs.Tab> tabs = DefaultTabs.load(gson);
		assertEquals(22, tabs.size());
		assertTrue(SortData.itemMeta().size() > 10000);
		for (DefaultTabs.Tab tab : tabs)
		{
			assertTrue(tab.getId() + " has a section structure", SortData.TAB_SECTIONS.containsKey(tab.getId()));
			assertTrue(tab.getId() + " has an icon", tab.getIcon() > 0);
			List<TabResolver.ResolvedSection> sections = TabResolver.resolve(tab.getItems(),
				SortData.TAB_SECTIONS.get(tab.getId()),
				id -> SortData.itemMeta().containsKey(id) && SortData.itemMeta().get(id).sections != null
					? SortData.itemMeta().get(id).sections.get(tab.getId()) : null,
				null);
			long placed = sections.stream().mapToLong(s -> s.getItems().size()).sum();
			long valid = tab.getItems().stream().filter(id -> id > 0).count();
			assertEquals(tab.getId() + ": every item lands in a section", valid, placed);
		}
	}
}

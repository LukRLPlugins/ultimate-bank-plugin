package com.ultimatebank;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

/**
 * One {@code @ConfigSection} per feature. Each section starts with an enable toggle,
 * followed by that feature's settings. Never rename a key without a migration.
 */
@ConfigGroup(UltimateBankConfig.GROUP)
public interface UltimateBankConfig extends Config
{
	String GROUP = "ultimatebank";

	// ---- Storage savings ----

	@ConfigSection(
		name = "Storage savings",
		description = "Find banked items that could be stored elsewhere, and which storage upgrades free the most bank space",
		position = 0
	)
	String storageSavingsSection = "storageSavingsSection";

	@ConfigSection(
		name = "Storage savings: locations",
		description = "Which storage locations to consider",
		position = 1,
		closedByDefault = true
	)
	String storageSavingsLocationsSection = "storageSavingsLocationsSection";

	@ConfigItem(
		keyName = "storageSavingsEnabled",
		name = "Enable",
		description = "Find banked items that could be stored elsewhere",
		section = storageSavingsSection,
		position = 0
	)
	default boolean storageSavingsEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsMarker",
		name = "Mark items in bank",
		description = "Mark storable items in the bank, with a tooltip naming where they can go",
		section = storageSavingsSection,
		position = 1
	)
	default boolean storageSavingsMarker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsMarkerColor",
		name = "Marker color",
		description = "Color of the marker on storable bank items",
		section = storageSavingsSection,
		position = 2
	)
	default Color storageSavingsMarkerColor()
	{
		return new Color(255, 200, 0);
	}

	@ConfigItem(
		keyName = "storageSavingsTagTab",
		name = "Storable tag tab",
		description = "Add a 'storable' bank tag tab that shows only storable items (requires the Bank Tags plugin)",
		section = storageSavingsSection,
		position = 3
	)
	default boolean storageSavingsTagTab()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsPanel",
		name = "Sidebar panel",
		description = "Show a sidebar panel ranking storage locations by how many bank slots they would free",
		section = storageSavingsSection,
		position = 4
	)
	default boolean storageSavingsPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsKeepBis",
		name = "Never flag best-in-slot",
		description = "Don't flag best-in-slot items such as skillcapes, max cape or Ancestral, even if they could be stored",
		section = storageSavingsSection,
		position = 5
	)
	default boolean storageSavingsKeepBis()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsIgnored",
		name = "Ignored items",
		description = "Comma-separated item names, item IDs or wildcards (e.g. *cape*) to never flag.<br>"
			+ "Tip: shift-right-click a marked bank item to ignore or unignore it.",
		section = storageSavingsSection,
		position = 6
	)
	default String storageSavingsIgnored()
	{
		return "";
	}

	@ConfigItem(
		keyName = "storageSavingsIgnored",
		name = "",
		description = ""
	)
	void storageSavingsIgnored(String value);

	@ConfigItem(
		keyName = "storageSavingsStash",
		name = "STASH units",
		description = "Flag items that could be stored in a clue scroll STASH unit",
		section = storageSavingsSection,
		position = 7
	)
	default boolean storageSavingsStash()
	{
		return false;
	}

	@ConfigItem(
		keyName = "storageSavingsStashBuiltOnly",
		name = "Only built, empty STASHes",
		description = "Only count STASH units you've built and haven't filled yet.<br>"
			+ "Open the STASH chart in your POH or Watson's noticeboard once to load which units are filled.",
		section = storageSavingsSection,
		position = 8
	)
	default boolean storageSavingsStashBuiltOnly()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsTreasureChest", name = "POH treasure chest", description = "Clue scroll rewards", section = storageSavingsLocationsSection, position = 0)
	default boolean storageSavingsTreasureChest()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsFancyDressBox", name = "POH fancy dress box", description = "Random event costumes", section = storageSavingsLocationsSection, position = 1)
	default boolean storageSavingsFancyDressBox()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsMagicWardrobe", name = "POH magic wardrobe", description = "Magic armour and robe sets", section = storageSavingsLocationsSection, position = 2)
	default boolean storageSavingsMagicWardrobe()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsArmourCase", name = "POH armour case", description = "Minigame and quest outfits", section = storageSavingsLocationsSection, position = 3)
	default boolean storageSavingsArmourCase()
	{
		return false;
	}

	@ConfigItem(keyName = "storageSavingsCapeRack", name = "POH cape rack", description = "Skillcapes and other capes", section = storageSavingsLocationsSection, position = 4)
	default boolean storageSavingsCapeRack()
	{
		return false;
	}

	@ConfigItem(keyName = "storageSavingsToyBox", name = "POH toy box", description = "Toys and holiday event items", section = storageSavingsLocationsSection, position = 5)
	default boolean storageSavingsToyBox()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsBookcase", name = "POH bookcase", description = "Quest books and journals", section = storageSavingsLocationsSection, position = 6)
	default boolean storageSavingsBookcase()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsPetHouse", name = "POH pet house", description = "Skilling and boss pets", section = storageSavingsLocationsSection, position = 7)
	default boolean storageSavingsPetHouse()
	{
		return false;
	}

	@ConfigItem(keyName = "storageSavingsSpiceRack", name = "POH spice rack", description = "Evil Dave's spices", section = storageSavingsLocationsSection, position = 8)
	default boolean storageSavingsSpiceRack()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsSeedVault", name = "Seed vault", description = "Seeds and saplings (Farming Guild)", section = storageSavingsLocationsSection, position = 9)
	default boolean storageSavingsSeedVault()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsToolLeprechaun", name = "Tool leprechaun", description = "Farming tools and supplies", section = storageSavingsLocationsSection, position = 10)
	default boolean storageSavingsToolLeprechaun()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsTackleBox", name = "Tackle box", description = "Fishing equipment and bait", section = storageSavingsLocationsSection, position = 11)
	default boolean storageSavingsTackleBox()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsHuntsmansKit", name = "Huntsman's kit", description = "Hunter equipment", section = storageSavingsLocationsSection, position = 12)
	default boolean storageSavingsHuntsmansKit()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsForestryKit", name = "Forestry kit", description = "Forestry items", section = storageSavingsLocationsSection, position = 13)
	default boolean storageSavingsForestryKit()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsSteelKeyRing", name = "Steel key ring", description = "Keys (One Small Favour reward)", section = storageSavingsLocationsSection, position = 14)
	default boolean storageSavingsSteelKeyRing()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsMasterScrollBook", name = "Master scroll book", description = "Teleport scrolls", section = storageSavingsLocationsSection, position = 15)
	default boolean storageSavingsMasterScrollBook()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsFossilStorage", name = "Fossil storage", description = "Fossils (Fossil Island and Varrock Museum)", section = storageSavingsLocationsSection, position = 16)
	default boolean storageSavingsFossilStorage()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsElnockInquisitor", name = "Elnock Inquisitor", description = "Puro-Puro equipment", section = storageSavingsLocationsSection, position = 17)
	default boolean storageSavingsElnockInquisitor()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsFlamtaerBag", name = "Flamtaer bag", description = "Shades of Mort'ton temple supplies", section = storageSavingsLocationsSection, position = 18)
	default boolean storageSavingsFlamtaerBag()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsNightmareZone", name = "Nightmare Zone", description = "NMZ potions", section = storageSavingsLocationsSection, position = 19)
	default boolean storageSavingsNightmareZone()
	{
		return true;
	}

	@ConfigItem(keyName = "storageSavingsMysteriousStranger", name = "Mysterious stranger", description = "Hallowed Sepulchre items", section = storageSavingsLocationsSection, position = 20)
	default boolean storageSavingsMysteriousStranger()
	{
		return true;
	}

	// ---- Layout tools ----

	@ConfigSection(
		name = "Layout tools",
		description = "Extra tools for core bank tag layouts: zigzag auto-layout and row editing",
		position = 2
	)
	String layoutToolsSection = "layoutToolsSection";

	@ConfigItem(
		keyName = "layoutToolsEnabled",
		name = "Enable",
		description = "Add extra tools to bank tag layouts (requires the Bank Tags plugin)",
		section = layoutToolsSection,
		position = 0
	)
	default boolean layoutToolsEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "layoutToolsZigzag",
		name = "Zigzag auto-layout",
		description = "Add a Zigzag style to a tag tab's 'Auto layout' right-click options. It lays out your gear, then your inventory, "
			+ "zigzagging down and up each column of a pair of rows.",
		section = layoutToolsSection,
		position = 1
	)
	default boolean layoutToolsZigzag()
	{
		return true;
	}

	@ConfigItem(
		keyName = "layoutToolsZigzagDuplicateLimit",
		name = "Zigzag: duplicate limit",
		description = "Inventory items repeated up to this many times in a row get one bank slot each (e.g. 4 prayer potions = 4 slots). "
			+ "Longer runs (e.g. 20 sharks) get a single slot. 0 = never duplicate.",
		section = layoutToolsSection,
		position = 2
	)
	@Range(max = 28)
	default int layoutToolsZigzagDuplicateLimit()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "layoutToolsZigzagRunePouch",
		name = "Zigzag: rune pouch runes",
		description = "Put the runes in your rune pouch on their own row",
		section = layoutToolsSection,
		position = 3
	)
	default boolean layoutToolsZigzagRunePouch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "layoutToolsRowMenus",
		name = "Insert/remove row menus",
		description = "Shift-right-click empty space in a laid-out tag tab to insert an empty row or remove an empty row",
		section = layoutToolsSection,
		position = 4
	)
	default boolean layoutToolsRowMenus()
	{
		return true;
	}
}

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
		description = "Add a 'storable' bank tag tab that shows only storable items (requires the Bank Tags plugin). "
			+ "With Sorted tabs on, it becomes 'storable (ub)', with a titled section per storage location.",
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

	// ---- Sorted tabs ----

	@ConfigSection(
		name = "Sorted tabs",
		description = "Auto-sorted skill tabs with titled sections that you can customize",
		position = 2
	)
	String sortedTabsSection = "sortedTabsSection";

	@ConfigSection(
		name = "Sorted tabs: which tabs",
		description = "Which sorted tabs to show",
		position = 3,
		closedByDefault = true
	)
	String sortedTabsTabsSection = "sortedTabsTabsSection";

	@ConfigItem(
		keyName = "sortedTabsEnabled",
		name = "Enable",
		description = "Add auto-sorted skill tabs, named like 'melee (ub)', to your bank tag tabs (requires the Bank Tags plugin)",
		section = sortedTabsSection,
		position = 0
	)
	default boolean sortedTabsEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "sortedTabsTitles",
		name = "Section titles",
		description = "Show a title and divider line above each section. Off = compact tabs with no blank title rows",
		section = sortedTabsSection,
		position = 1
	)
	default boolean sortedTabsTitles()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sortedTabsLowerTier",
		name = "Lower tier at bottom",
		description = "In melee, range and mage, move lower-level gear to a 'Lower tier' group at the bottom",
		section = sortedTabsSection,
		position = 2
	)
	default boolean sortedTabsLowerTier()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sortedTabsLowerTierCount",
		name = "Loadout tiers kept",
		description = "How many of the highest requirement levels per section stay at the top before the rest count as lower tier",
		section = sortedTabsSection,
		position = 3
	)
	@Range(min = 1, max = 10)
	default int sortedTabsLowerTierCount()
	{
		return 3;
	}

	@ConfigItem(keyName = "sortedTabsMelee", name = "Melee", description = "Show the melee tab", section = sortedTabsTabsSection, position = 0)
	default boolean sortedTabsMelee()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsRange", name = "Range", description = "Show the range tab", section = sortedTabsTabsSection, position = 1)
	default boolean sortedTabsRange()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsMage", name = "Mage", description = "Show the mage tab", section = sortedTabsTabsSection, position = 2)
	default boolean sortedTabsMage()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsPrayer", name = "Prayer", description = "Show the prayer tab", section = sortedTabsTabsSection, position = 3)
	default boolean sortedTabsPrayer()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsTeleports", name = "Teleports", description = "Show the teleports tab", section = sortedTabsTabsSection, position = 4)
	default boolean sortedTabsTeleports()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsCooking", name = "Cooking", description = "Show the cooking tab", section = sortedTabsTabsSection, position = 5)
	default boolean sortedTabsCooking()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsWoodcuttingFiremaking", name = "Woodcutting + Firemaking", description = "Show the woodcutting + firemaking tab", section = sortedTabsTabsSection, position = 6)
	default boolean sortedTabsWoodcuttingFiremaking()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsFletching", name = "Fletching", description = "Show the fletching tab", section = sortedTabsTabsSection, position = 7)
	default boolean sortedTabsFletching()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsFishing", name = "Fishing", description = "Show the fishing tab", section = sortedTabsTabsSection, position = 8)
	default boolean sortedTabsFishing()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsCrafting", name = "Crafting", description = "Show the crafting tab", section = sortedTabsTabsSection, position = 9)
	default boolean sortedTabsCrafting()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsMiningSmithing", name = "Mining + Smithing", description = "Show the mining + smithing tab", section = sortedTabsTabsSection, position = 10)
	default boolean sortedTabsMiningSmithing()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsHerblore", name = "Herblore", description = "Show the herblore tab", section = sortedTabsTabsSection, position = 11)
	default boolean sortedTabsHerblore()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsAgilityThieving", name = "Agility + Thieving", description = "Show the agility + thieving tab", section = sortedTabsTabsSection, position = 12)
	default boolean sortedTabsAgilityThieving()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsSlayer", name = "Slayer", description = "Show the slayer tab", section = sortedTabsTabsSection, position = 13)
	default boolean sortedTabsSlayer()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsFarming", name = "Farming", description = "Show the farming tab", section = sortedTabsTabsSection, position = 14)
	default boolean sortedTabsFarming()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsRunecraft", name = "Runecraft", description = "Show the runecraft tab", section = sortedTabsTabsSection, position = 15)
	default boolean sortedTabsRunecraft()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsHunter", name = "Hunter", description = "Show the hunter tab", section = sortedTabsTabsSection, position = 16)
	default boolean sortedTabsHunter()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsConstruction", name = "Construction", description = "Show the construction tab", section = sortedTabsTabsSection, position = 17)
	default boolean sortedTabsConstruction()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsSailing", name = "Sailing", description = "Show the sailing tab", section = sortedTabsTabsSection, position = 18)
	default boolean sortedTabsSailing()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsQuests", name = "Quests", description = "Show the quests tab", section = sortedTabsTabsSection, position = 19)
	default boolean sortedTabsQuests()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsMisc", name = "Misc", description = "Show the misc tab", section = sortedTabsTabsSection, position = 20)
	default boolean sortedTabsMisc()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsCosmetics", name = "Cosmetics", description = "Show the cosmetics tab", section = sortedTabsTabsSection, position = 21)
	default boolean sortedTabsCosmetics()
	{
		return true;
	}

	@ConfigItem(keyName = "sortedTabsStorable", name = "Storable", description = "Show the storable tab: banked items that could be stored elsewhere, one section per storage location (needs Storage savings and its 'Storable tag tab' option)", section = sortedTabsTabsSection, position = 22)
	default boolean sortedTabsStorable()
	{
		return true;
	}
}

package com.ultimatebank;

import com.ultimatebank.feature.banksearch.ValueType;
import com.ultimatebank.feature.storagesavings.marker.MarkerStyle;
import com.ultimatebank.feature.storagesavings.tiers.TierChoice;
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
		name = "Bank Savings: General",
		description = "Find banked items that could be stored elsewhere, and which storage upgrades free the most bank space",
		position = 0
	)
	String storageSavingsSection = "storageSavingsSection";

	@ConfigSection(
		name = "Bank Savings: Locations",
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
		keyName = "storageSavingsMarkerStyle",
		name = "Marker style",
		description = "How storable bank items are marked",
		section = storageSavingsSection,
		position = 3
	)
	default MarkerStyle storageSavingsMarkerStyle()
	{
		return MarkerStyle.CORNER_TRIANGLE;
	}

	@ConfigItem(
		keyName = "storageSavingsMarkerCorner",
		name = "Marker corner",
		description = "Which corner the triangle or icon goes in",
		section = storageSavingsSection,
		position = 4
	)
	default MarkerStyle.Corner storageSavingsMarkerCorner()
	{
		return MarkerStyle.Corner.BOTTOM_RIGHT;
	}

	@ConfigItem(
		keyName = "storageSavingsMarkerSize",
		name = "Marker size",
		description = "Size of the corner triangle, in pixels",
		section = storageSavingsSection,
		position = 5
	)
	@Range(min = 4, max = 24)
	default int storageSavingsMarkerSize()
	{
		return 8;
	}

	@ConfigItem(
		keyName = "storageSavingsMarkerIconSize",
		name = "Icon size",
		description = "Size of the corner icon, in pixels",
		section = storageSavingsSection,
		position = 6
	)
	@Range(min = 10, max = 32)
	default int storageSavingsMarkerIconSize()
	{
		return 16;
	}

	@ConfigItem(
		keyName = "storageSavingsMarkerThickness",
		name = "Outline thickness",
		description = "Thickness of the item or box outline, in pixels",
		section = storageSavingsSection,
		position = 7
	)
	@Range(min = 1, max = 4)
	default int storageSavingsMarkerThickness()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "storageSavingsTagTab",
		name = "Storable tag tab",
		description = "Add a 'storable' bank tag tab that shows only storable items (requires the Bank Tags plugin)",
		section = storageSavingsSection,
		position = 8
	)
	default boolean storageSavingsTagTab()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsTagTabGroups",
		name = "Group storable tab",
		description = "In the 'storable' tab, group items by the container they fit in, with a title above each group "
			+ "(most bank slots freed first). An item that fits several containers shows in each group.",
		section = storageSavingsSection,
		position = 9
	)
	default boolean storageSavingsTagTabGroups()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsUseSnapshots",
		name = "Use saved snapshots",
		description = "Remember your bank and POH storage contents. Before you open the bank, work from the bank as it was "
			+ "last seen; show how many copies a POH storage already holds; and count sets already stored toward a "
			+ "storage's tier capacity. Open each POH storage once so it's recorded.",
		section = storageSavingsSection,
		position = 10
	)
	default boolean storageSavingsUseSnapshots()
	{
		return true;
	}

	@ConfigItem(
		keyName = "storageSavingsPanel",
		name = "Sidebar panel",
		description = "Show a sidebar panel ranking storage locations by how many bank slots they would free",
		section = storageSavingsSection,
		position = 11
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
		position = 12
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
		position = 13
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
		position = 14
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
		position = 15
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

	// ---- Storage savings: POH tiers ----

	@ConfigSection(
		name = "Bank Savings: Tiers",
		description = "Which tier of each costume room storage you've built, so only what fits is suggested. "
			+ "Auto-detect reads it when you're in your costume room.",
		position = 2,
		closedByDefault = true
	)
	String storageSavingsTiersSection = "storageSavingsTiersSection";

	@ConfigItem(keyName = "storageSavingsTierFancyDressBox", name = "Fancy dress box", description = "Oak holds 2 costumes, teak 4, mahogany all", section = storageSavingsTiersSection, position = 0)
	default TierChoice.FancyDressBox storageSavingsTierFancyDressBox()
	{
		return TierChoice.FancyDressBox.AUTO_DETECT;
	}

	@ConfigItem(keyName = "storageSavingsTierArmourCase", name = "Armour case", description = "Oak holds 25 sets, teak 50, mahogany all", section = storageSavingsTiersSection, position = 1)
	default TierChoice.ArmourCase storageSavingsTierArmourCase()
	{
		return TierChoice.ArmourCase.AUTO_DETECT;
	}

	@ConfigItem(keyName = "storageSavingsTierMagicWardrobe", name = "Magic wardrobe", description = "Oak holds 7 sets, +7 per tier, marble all", section = storageSavingsTiersSection, position = 2)
	default TierChoice.MagicWardrobe storageSavingsTierMagicWardrobe()
	{
		return TierChoice.MagicWardrobe.AUTO_DETECT;
	}

	@ConfigItem(keyName = "storageSavingsTierCapeRack", name = "Cape rack", description = "All capes fit; skillcapes: oak 0, teak 1, mahogany 5, gilded 10, marble/magical all", section = storageSavingsTiersSection, position = 3)
	default TierChoice.CapeRack storageSavingsTierCapeRack()
	{
		return TierChoice.CapeRack.AUTO_DETECT;
	}

	@ConfigItem(keyName = "storageSavingsTierTreasureChest", name = "Treasure chest", description = "Oak: beginner and easy rewards, teak: + medium, mahogany: all", section = storageSavingsTiersSection, position = 4)
	default TierChoice.TreasureChest storageSavingsTierTreasureChest()
	{
		return TierChoice.TreasureChest.AUTO_DETECT;
	}

	// ---- Ultimate Bank Search ----

	@ConfigSection(
		name = "Bank Search: General",
		description = "A sidebar panel to search your bank and POH storage, and see what each visit changed",
		position = 3
	)
	String bankSearchSection = "bankSearchSection";

	@ConfigItem(
		keyName = "bankSearchEnabled",
		name = "Enable",
		description = "Add a sidebar panel to search your bank and POH storage (open each once so it's recorded)",
		section = bankSearchSection,
		position = 0
	)
	default boolean bankSearchEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankSearchSnapshotCount",
		name = "Visits kept",
		description = "How many snapshots of each location to keep (one per visit), for the history",
		section = bankSearchSection,
		position = 1
	)
	@Range(min = 2, max = 20)
	default int bankSearchSnapshotCount()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "bankSearchWarnHours",
		name = "Old after (hours)",
		description = "Show when a location was last seen in orange once it's this old",
		section = bankSearchSection,
		position = 2
	)
	default int bankSearchWarnHours()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "bankSearchStaleHours",
		name = "Stale after (hours)",
		description = "Show when a location was last seen in red once it's this old",
		section = bankSearchSection,
		position = 3
	)
	default int bankSearchStaleHours()
	{
		return 168;
	}

	@ConfigItem(
		keyName = "bankSearchValueType",
		name = "Value items at",
		description = "Price used for item values and totals in the panel (also switchable there).<br>"
			+ "GE price uses high alch value for untradeables.",
		section = bankSearchSection,
		position = 4
	)
	default ValueType bankSearchValueType()
	{
		return ValueType.GE;
	}

	// ---- Bank Tweaks ----

	@ConfigSection(
		name = "Bank Tweaks",
		description = "Small changes to what clicks in the bank do",
		position = 4,
		closedByDefault = true
	)
	String bankTweaksSection = "bankTweaksSection";

	@ConfigItem(
		keyName = "bankTweaksEnabled",
		name = "Enable",
		description = "Turn on the bank tweaks below",
		section = bankTweaksSection,
		position = 0
	)
	default boolean bankTweaksEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bankTweaksViewTagTabs",
		name = "Left-click: view tag tabs",
		description = "Left-clicking the bank's \"New tag tab\" button lists your tag tabs instead.<br>"
			+ "Right-click it to make a new tab.",
		section = bankTweaksSection,
		position = 1
	)
	default boolean bankTweaksViewTagTabs()
	{
		return true;
	}
}

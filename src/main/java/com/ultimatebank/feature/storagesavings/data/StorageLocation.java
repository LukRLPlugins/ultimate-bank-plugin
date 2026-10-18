package com.ultimatebank.feature.storagesavings.data;

import com.ultimatebank.UltimateBankConfig;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.Getter;

/**
 * Places other than the bank that can hold items. Item lists are ported from Wasted Bank Space
 * (see docs/attribution.md); add new items to the matching {@code *Items} class.
 */
public enum StorageLocation
{
	TREASURE_CHEST("POH treasure chest", true, TreasureChestItems.ITEMS, null, UltimateBankConfig::storageSavingsTreasureChest),
	FANCY_DRESS_BOX("POH fancy dress box", true, FancyDressBoxItems.ITEMS, null, UltimateBankConfig::storageSavingsFancyDressBox),
	MAGIC_WARDROBE("POH magic wardrobe", true, MagicWardrobeItems.ITEMS, MagicWardrobeItems.BIS, UltimateBankConfig::storageSavingsMagicWardrobe),
	ARMOUR_CASE("POH armour case", true, ArmourCaseItems.ITEMS, ArmourCaseItems.BIS, UltimateBankConfig::storageSavingsArmourCase),
	CAPE_RACK("POH cape rack", true, CapeRackItems.ITEMS, CapeRackItems.BIS, UltimateBankConfig::storageSavingsCapeRack),
	TOY_BOX("POH toy box", true, ToyBoxItems.ITEMS, null, UltimateBankConfig::storageSavingsToyBox),
	BOOKCASE("POH bookcase", true, BookcaseItems.ITEMS, null, UltimateBankConfig::storageSavingsBookcase),
	PET_HOUSE("POH pet house", true, PetHouseItems.ITEMS, null, UltimateBankConfig::storageSavingsPetHouse),
	SPICE_RACK("POH spice rack", true, SpiceRackItems.ITEMS, null, UltimateBankConfig::storageSavingsSpiceRack),
	SEED_VAULT("Seed vault", false, SeedVaultItems.ITEMS, null, UltimateBankConfig::storageSavingsSeedVault),
	TOOL_LEPRECHAUN("Tool leprechaun", false, ToolLeprechaunItems.ITEMS, null, UltimateBankConfig::storageSavingsToolLeprechaun),
	TACKLE_BOX("Tackle box", false, TackleBoxItems.ITEMS, null, UltimateBankConfig::storageSavingsTackleBox),
	HUNTSMANS_KIT("Huntsman's kit", false, HuntsmansKitItems.ITEMS, null, UltimateBankConfig::storageSavingsHuntsmansKit),
	FORESTRY_KIT("Forestry kit", false, ForestryKitItems.ITEMS, null, UltimateBankConfig::storageSavingsForestryKit),
	STEEL_KEY_RING("Steel key ring", false, SteelKeyRingItems.ITEMS, null, UltimateBankConfig::storageSavingsSteelKeyRing),
	MASTER_SCROLL_BOOK("Master scroll book", false, MasterScrollBookItems.ITEMS, null, UltimateBankConfig::storageSavingsMasterScrollBook),
	FOSSIL_STORAGE("Fossil storage", false, FossilStorageItems.ITEMS, null, UltimateBankConfig::storageSavingsFossilStorage),
	ELNOCK_INQUISITOR("Elnock Inquisitor", false, ElnockInquisitorItems.ITEMS, null, UltimateBankConfig::storageSavingsElnockInquisitor),
	FLAMTAER_BAG("Flamtaer bag", false, FlamtaerBagItems.ITEMS, null, UltimateBankConfig::storageSavingsFlamtaerBag),
	NIGHTMARE_ZONE("Nightmare Zone", false, NightmareZoneItems.ITEMS, null, UltimateBankConfig::storageSavingsNightmareZone),
	MYSTERIOUS_STRANGER("Mysterious stranger", false, MysteriousStrangerItems.ITEMS, null, UltimateBankConfig::storageSavingsMysteriousStranger);

	private static final Map<Integer, Set<StorageLocation>> LOCATIONS_BY_ITEM = new HashMap<>();
	private static final Set<Integer> BIS_ITEMS = new HashSet<>();

	static
	{
		for (StorageLocation location : values())
		{
			for (int itemId : location.itemIds)
			{
				LOCATIONS_BY_ITEM.computeIfAbsent(itemId, id -> EnumSet.noneOf(StorageLocation.class)).add(location);
			}
			if (location.bisItemIds != null)
			{
				for (int itemId : location.bisItemIds)
				{
					BIS_ITEMS.add(itemId);
				}
			}
		}
	}

	@Getter
	private final String displayName;
	/** Whether this is a player-owned house build, i.e. an upgrade the player can build to gain space */
	@Getter
	private final boolean poh;
	private final int[] itemIds;
	private final int[] bisItemIds;
	private final Predicate<UltimateBankConfig> enabled;

	StorageLocation(String displayName, boolean poh, int[] itemIds, int[] bisItemIds, Predicate<UltimateBankConfig> enabled)
	{
		this.displayName = displayName;
		this.poh = poh;
		this.itemIds = itemIds;
		this.bisItemIds = bisItemIds;
		this.enabled = enabled;
	}

	public boolean isEnabled(UltimateBankConfig config)
	{
		return enabled.test(config);
	}

	/**
	 * @return every location that can hold the item, enabled or not
	 */
	public static Set<StorageLocation> forItem(int itemId)
	{
		return LOCATIONS_BY_ITEM.getOrDefault(itemId, Collections.emptySet());
	}

	/**
	 * @return whether some location marks the item as best-in-slot, i.e. worth keeping at hand
	 */
	public static boolean isBis(int itemId)
	{
		return BIS_ITEMS.contains(itemId);
	}

	/**
	 * @return every item any location can hold
	 */
	public static Set<Integer> allItemIds()
	{
		return Collections.unmodifiableSet(LOCATIONS_BY_ITEM.keySet());
	}
}

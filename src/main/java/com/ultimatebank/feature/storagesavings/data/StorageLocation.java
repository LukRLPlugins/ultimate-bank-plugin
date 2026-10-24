package com.ultimatebank.feature.storagesavings.data;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import lombok.Getter;
import net.runelite.api.gameval.ItemID;

/**
 * Places other than the bank that can hold items. Item lists are ported from Wasted Bank Space, and the costume room
 * storages also get every item the OSRS Wiki lists ({@link WikiStorageItems}, generated; see docs/attribution.md).
 * Add new items to the matching {@code *Items} class, or regenerate the wiki data.
 */
public enum StorageLocation
{
	TREASURE_CHEST("POH treasure chest", true, merge(TreasureChestItems.ITEMS, WikiStorageItems.TREASURE_CHEST), null, UltimateBankConfig::storageSavingsTreasureChest),
	FANCY_DRESS_BOX("POH fancy dress box", true, merge(FancyDressBoxItems.ITEMS, WikiStorageItems.FANCY_DRESS_BOX), null, UltimateBankConfig::storageSavingsFancyDressBox),
	MAGIC_WARDROBE("POH magic wardrobe", true, merge(MagicWardrobeItems.ITEMS, WikiStorageItems.MAGIC_WARDROBE), MagicWardrobeItems.BIS, UltimateBankConfig::storageSavingsMagicWardrobe),
	ARMOUR_CASE("POH armour case", true, merge(ArmourCaseItems.ITEMS, WikiStorageItems.ARMOUR_CASE), ArmourCaseItems.BIS, UltimateBankConfig::storageSavingsArmourCase),
	CAPE_RACK("POH cape rack", true, merge(CapeRackItems.ITEMS, WikiStorageItems.CAPE_RACK), CapeRackItems.BIS, UltimateBankConfig::storageSavingsCapeRack),
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

	/**
	 * @return both lists' items, each once (used to add the OSRS Wiki's items to the hand-kept lists)
	 */
	private static int[] merge(int[] a, int[] b)
	{
		return IntStream.concat(Arrays.stream(a), Arrays.stream(b)).distinct().toArray();
	}

	public boolean isEnabled(UltimateBankConfig config)
	{
		return enabled.test(config);
	}

	/**
	 * @return every item this location can hold
	 */
	public int[] getItemIds()
	{
		return itemIds.clone();
	}

	/**
	 * @return the snapshot that records what this location already holds, or null if it isn't snapshotted
	 */
	public SnapshotLocation getSnapshotLocation()
	{
		switch (this)
		{
			case TREASURE_CHEST:
				return SnapshotLocation.TREASURE_CHEST;
			case FANCY_DRESS_BOX:
				return SnapshotLocation.FANCY_DRESS_BOX;
			case MAGIC_WARDROBE:
				return SnapshotLocation.MAGIC_WARDROBE;
			case ARMOUR_CASE:
				return SnapshotLocation.ARMOUR_CASE;
			case CAPE_RACK:
				return SnapshotLocation.CAPE_RACK;
			case TOY_BOX:
				return SnapshotLocation.TOY_BOX;
			case BOOKCASE:
				return SnapshotLocation.BOOKCASE;
			case PET_HOUSE:
				return SnapshotLocation.PET_HOUSE;
			case SEED_VAULT:
				return SnapshotLocation.SEED_VAULT;
			case TACKLE_BOX:
				return SnapshotLocation.TACKLE_BOX;
			case HUNTSMANS_KIT:
				return SnapshotLocation.HUNTSMANS_KIT;
			case FORESTRY_KIT:
				return SnapshotLocation.FORESTRY_KIT;
			case TOOL_LEPRECHAUN:
				return SnapshotLocation.TOOL_LEPRECHAUN;
			case FOSSIL_STORAGE:
				return SnapshotLocation.FOSSIL_STORAGE;
			case MASTER_SCROLL_BOOK:
				return SnapshotLocation.MASTER_SCROLL_BOOK;
			case NIGHTMARE_ZONE:
				return SnapshotLocation.NIGHTMARE_ZONE;
			case ELNOCK_INQUISITOR:
				return SnapshotLocation.ELNOCK_INQUISITOR;
			case MYSTERIOUS_STRANGER:
				return SnapshotLocation.MYSTERIOUS_STRANGER;
			case STEEL_KEY_RING:
				return SnapshotLocation.STEEL_KEY_RING;
			default:
				return null;
		}
	}

	/**
	 * @return an item whose sprite stands for this location (the corner icon marker)
	 */
	public int getIconItemId()
	{
		switch (this)
		{
			case TREASURE_CHEST:
				return ItemID.TRAIL_REWARD_CASKET_MASTER;
			case FANCY_DRESS_BOX:
				return ItemID.MACRO_FROG_MASK;
			case MAGIC_WARDROBE:
				return ItemID.ANCESTRAL_HAT;
			case ARMOUR_CASE:
				return ItemID.CASTLEWARS_FULL_HELM;
			case CAPE_RACK:
				return ItemID.SKILLCAPE_MAX;
			case TOY_BOX:
				return ItemID.XMAS_YOYO;
			case BOOKCASE:
				return ItemID.THE_SHIELD_OF_ARRAV;
			case PET_HOUSE:
				return ItemID.KITTENOBJECT;
			case SPICE_RACK:
				return ItemID.HUNDRED_DAVE_SPICE_RED_4;
			case SEED_VAULT:
				return ItemID.SEED_BOX;
			case TOOL_LEPRECHAUN:
				return ItemID.RAKE;
			case TACKLE_BOX:
				return ItemID.TACKLE_BOX;
			case HUNTSMANS_KIT:
				return ItemID.HUNTSMANS_KIT;
			case FORESTRY_KIT:
				return ItemID.FORESTRY_KIT;
			case STEEL_KEY_RING:
				return ItemID.FAVOUR_KEY_RING;
			case MASTER_SCROLL_BOOK:
				return ItemID.BOOKOFSCROLLS_CHARGED;
			case FOSSIL_STORAGE:
				return ItemID.FOSSIL_LARGE_UNID;
			case ELNOCK_INQUISITOR:
				return ItemID.II_IMPLING_JAR;
			case FLAMTAER_BAG:
				return ItemID.FLAMTAER_BAG;
			case NIGHTMARE_ZONE:
				return ItemID.NZONE4DOSEABSORPTIONPOTION;
			case MYSTERIOUS_STRANGER:
			default:
				return ItemID.HALLOWED_TOKEN;
		}
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

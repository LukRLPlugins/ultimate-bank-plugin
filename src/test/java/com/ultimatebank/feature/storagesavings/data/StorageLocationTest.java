package com.ultimatebank.feature.storagesavings.data;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.ultimatebank.feature.storagesavings.stash.StashUnit;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

public class StorageLocationTest
{
	@Test
	public void everyLocationHoldsItems()
	{
		for (StorageLocation location : StorageLocation.values())
		{
			assertTrue(location + " has no items", StorageLocation.allItemIds().stream()
				.anyMatch(itemId -> StorageLocation.forItem(itemId).contains(location)));
		}
		assertTrue(StorageLocation.allItemIds().size() > 2000);
	}

	@Test
	public void spotChecks()
	{
		assertTrue(StorageLocation.forItem(ItemID.SAILING_FINE_FISH_OFFCUTS).contains(StorageLocation.TACKLE_BOX));
		assertTrue(StorageLocation.forItem(ItemID.TRAIL_SARADOMIN_CLOAK).contains(StorageLocation.TREASURE_CHEST));
		assertTrue(StorageLocation.forItem(ItemID.ROSEWOOD_SEED).contains(StorageLocation.SEED_VAULT));
		assertFalse("Fish chunks can't go in the tackle box",
			StorageLocation.forItem(ItemID.FISH_CHUNKS).contains(StorageLocation.TACKLE_BOX));
	}

	@Test
	public void wikiItemsFillTheGaps()
	{
		// Missing from the hand-kept lists before the OSRS Wiki data was merged in (#20)
		assertTrue(StorageLocation.forItem(ItemID.ABYSSAL_LANTERN).contains(StorageLocation.MAGIC_WARDROBE));
		assertTrue(StorageLocation.forItem(ItemID.ABYSSAL_LANTERN_MAGIC).contains(StorageLocation.MAGIC_WARDROBE));
		// And nothing from the hand-kept lists was dropped
		assertTrue(StorageLocation.forItem(ItemID.TRAIL_SARADOMIN_CLOAK).contains(StorageLocation.TREASURE_CHEST));
	}

	@Test
	public void bisItemsAreStorable()
	{
		assertTrue(StorageLocation.isBis(ItemID.SKILLCAPE_MAX));
		assertFalse(StorageLocation.forItem(ItemID.SKILLCAPE_MAX).isEmpty());
	}

	@Test
	public void stashUnitsAreIndexed()
	{
		assertFalse(StashUnit.forItem(ItemID.BRONZE_DAGGER).isEmpty());
	}
}

package com.ultimatebank.core.snapshots;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;

/**
 * Storages the game sends as an item container while their interface is open, so they're read like the bank.
 */
enum ContainerStorage
{
	PET_HOUSE(SnapshotLocation.PET_HOUSE, InventoryID.POH_MENAGERIE_PETS, InterfaceID.POH_MENAGERIE),
	SEED_VAULT(SnapshotLocation.SEED_VAULT, InventoryID.SEED_VAULT, InterfaceID.SEED_VAULT),
	TACKLE_BOX(SnapshotLocation.TACKLE_BOX, InventoryID.TACKLE_BOX, InterfaceID.TACKLE_BOX_MAIN),
	HUNTSMANS_KIT(SnapshotLocation.HUNTSMANS_KIT, InventoryID.HUNTSMANS_KIT, InterfaceID.HUNTSMANS_KIT),
	FORESTRY_KIT(SnapshotLocation.FORESTRY_KIT, InventoryID.FORESTRY_KIT, InterfaceID.FORESTRY_KIT_MAIN);

	final SnapshotLocation location;
	final int inventoryId;
	/** Interface group shown while it's open */
	final int interfaceId;

	ContainerStorage(SnapshotLocation location, int inventoryId, int interfaceId)
	{
		this.location = location;
		this.inventoryId = inventoryId;
		this.interfaceId = interfaceId;
	}

	/**
	 * @return the storage with this item container, or null
	 */
	static ContainerStorage forInventory(int inventoryId)
	{
		for (ContainerStorage storage : values())
		{
			if (storage.inventoryId == inventoryId)
			{
				return storage;
			}
		}
		return null;
	}

	/**
	 * @return the storage recorded as this location, or null
	 */
	static ContainerStorage forLocation(SnapshotLocation location)
	{
		for (ContainerStorage storage : values())
		{
			if (storage.location == location)
			{
				return storage;
			}
		}
		return null;
	}

	/**
	 * @return the storage shown by this interface group, or null
	 */
	static ContainerStorage forInterface(int interfaceId)
	{
		for (ContainerStorage storage : values())
		{
			if (storage.interfaceId == interfaceId)
			{
				return storage;
			}
		}
		return null;
	}
}

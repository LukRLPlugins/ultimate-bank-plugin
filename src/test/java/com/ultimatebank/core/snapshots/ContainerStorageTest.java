package com.ultimatebank.core.snapshots;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Arrays;
import org.junit.Test;

public class ContainerStorageTest
{
	@Test
	public void eachStorageIsFoundByItsContainerAndInterface()
	{
		for (ContainerStorage storage : ContainerStorage.values())
		{
			assertEquals(storage, ContainerStorage.forInventory(storage.inventoryId));
			assertEquals(storage, ContainerStorage.forInterface(storage.interfaceId));
		}
	}

	@Test
	public void eachStorageHasItsOwnLocation()
	{
		assertEquals(ContainerStorage.values().length,
			Arrays.stream(ContainerStorage.values()).map(s -> s.location).distinct().count());
	}

	@Test
	public void otherIdsAreIgnored()
	{
		assertNull(ContainerStorage.forInventory(net.runelite.api.gameval.InventoryID.BANK));
		assertNull(ContainerStorage.forInterface(net.runelite.api.gameval.InterfaceID.BANKMAIN));
	}
}

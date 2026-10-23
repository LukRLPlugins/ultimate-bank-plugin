/*
 * Copyright (c) 2024, Adam <Adam@sigterm.info>
 * Copyright (c) 2026, Luk
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ultimatebank.core.snapshots;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ScriptID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;

/**
 * Reads the bank's potion store: doses of each potion, worked out by the game's own script, plus empty vials.
 * Ported from RuneLite's bank tags PotionStorage (Adam, BSD-2), which isn't usable from a plugin.
 * Must run on the client thread, outside any script callback (it runs scripts itself).
 */
@Singleton
class PotionStorageReader
{
	private static final int MAX_DOSES = 4;

	private final Client client;

	@Inject
	PotionStorageReader(Client client)
	{
		this.client = client;
	}

	/**
	 * @return item ID to quantity, each potion as its fewest-dose form so no dose is lost, or null if the game's
	 * potion lists aren't loaded
	 */
	Map<Integer, Integer> read()
	{
		EnumComposition potions = client.getEnum(EnumID.POTIONSTORE_POTIONS);
		EnumComposition unfinished = client.getEnum(EnumID.POTIONSTORE_UNFINISHED_POTIONS);
		if (potions == null || unfinished == null)
		{
			return null;
		}

		Map<Integer, Integer> items = new HashMap<>();
		for (EnumComposition list : new EnumComposition[]{potions, unfinished})
		{
			for (int potionEnumId : list.getIntVals())
			{
				EnumComposition potion = client.getEnum(potionEnumId);
				if (potion == null)
				{
					continue;
				}
				client.runScript(ScriptID.POTIONSTORE_DOSES, potionEnumId);
				int doses = client.getIntStack()[0];
				addDoses(items, potion::getIntValue, doses);
			}
		}

		int vials = client.getVarpValue(VarPlayerID.POTIONSTORE_VIALS);
		if (vials > 0)
		{
			items.merge(ItemID.VIAL_EMPTY, vials, Integer::sum);
		}
		return items;
	}

	/**
	 * Adds a potion's doses as its fewest-dose item (unfinished potions only have one form).
	 *
	 * @param doseItem the potion's item for a dose count, or a value below 1 if it has none
	 */
	static void addDoses(Map<Integer, Integer> items, IntUnaryOperator doseItem, int doses)
	{
		if (doses <= 0)
		{
			return;
		}
		for (int dose = 1; dose <= MAX_DOSES; dose++)
		{
			int itemId = doseItem.applyAsInt(dose);
			if (itemId > 0)
			{
				int quantity = doses / dose;
				if (quantity > 0)
				{
					items.merge(itemId, quantity, Integer::sum);
				}
				return;
			}
		}
	}
}

/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2026, Wozak-1
 * Copyright (c) 2026, Luk
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ultimatebank.core.snapshots;

import com.google.common.base.Strings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

/**
 * Reads a player-owned house costume room storage interface: which storage it is (from its text) and which items
 * are stored in it. Adapted from Where's My Stuff by Wozak-1. Must be called on the client thread.
 */
@Slf4j
@Singleton
class PohStorageScanner
{
	@Value
	static class Result
	{
		SnapshotLocation location;
		Map<Integer, Integer> quantities;
		/** Text of the interface's tier label, if any (logged to work out tier detection) */
		String tierText;
	}

	private final Client client;

	@Inject
	PohStorageScanner(Client client)
	{
		this.client = client;
	}

	/**
	 * @return what the open costume room interface holds, or null if none is open or it isn't populated yet
	 */
	Result scan()
	{
		Widget universe = client.getWidget(InterfaceID.PohCostumes.UNIVERSE);
		Widget items = client.getWidget(InterfaceID.PohCostumes.ITEMS);
		if (universe == null || items == null)
		{
			return null;
		}

		StringBuilder text = new StringBuilder();
		for (Widget widget : traverse(universe))
		{
			append(text, widget.getText());
			append(text, widget.getName());
		}
		SnapshotLocation location = SnapshotLocation.fromInterfaceText(text.toString());
		if (location == null)
		{
			return null;
		}

		Map<Integer, Integer> quantities = new HashMap<>();
		for (Widget widget : traverse(items))
		{
			// Stored items are drawn opaque; ones you could store but haven't are faded
			if (widget.isHidden() || widget.getItemId() <= 0 || widget.getOpacity() > 0)
			{
				continue;
			}
			quantities.put(widget.getItemId(), Math.max(1, widget.getItemQuantity()));
		}

		Widget tier = client.getWidget(InterfaceID.PohCostumes.TIER);
		return new Result(location, quantities, tier == null ? null : Text.removeTags(tier.getText()));
	}

	private static void append(StringBuilder text, String value)
	{
		if (value != null && !value.isEmpty())
		{
			text.append(' ').append(Text.removeTags(value));
		}
	}

	/**
	 * Pets in the house menagerie. They're kept as varbits, not in the menagerie's item container (that only holds
	 * pet items), but the open interface lists each one.
	 *
	 * @return item ID of each pet listed, or null if the menagerie isn't open
	 */
	Map<Integer, Integer> scanMenagerie()
	{
		Widget list = client.getWidget(InterfaceID.PohMenagerie.LIST);
		if (list == null)
		{
			return null;
		}

		Map<Integer, Integer> pets = new HashMap<>();
		int other = 0;
		for (Widget widget : traverse(list))
		{
			if (widget.isHidden())
			{
				continue;
			}
			if (widget.getItemId() > 0)
			{
				pets.put(widget.getItemId(), 1);
			}
			else if (widget != list && (widget.getModelId() > 0 || !Strings.isNullOrEmpty(widget.getName())
				|| !Strings.isNullOrEmpty(widget.getText())))
			{
				// Not an item icon: logged to learn how the list shows pets
				if (other++ < 20)
				{
					log.debug("Menagerie entry: type {}, model {}, name '{}', text '{}'", widget.getType(),
						widget.getModelId(), widget.getName(), widget.getText());
				}
			}
		}
		log.debug("Menagerie list: {} pets as items, {} other entries", pets.size(), other);
		return pets;
	}

	private static Iterable<Widget> traverse(Widget root)
	{
		Deque<Widget> queue = new ArrayDeque<>();
		Set<Widget> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		List<Widget> out = new ArrayList<>();
		queue.add(root);
		seen.add(root);
		while (!queue.isEmpty())
		{
			Widget widget = queue.removeFirst();
			out.add(widget);
			for (Widget[] children : new Widget[][]{widget.getChildren(), widget.getDynamicChildren(),
				widget.getStaticChildren(), widget.getNestedChildren()})
			{
				if (children == null)
				{
					continue;
				}
				for (Widget child : children)
				{
					if (child != null && seen.add(child))
					{
						queue.addLast(child);
					}
				}
			}
		}
		return out;
	}
}

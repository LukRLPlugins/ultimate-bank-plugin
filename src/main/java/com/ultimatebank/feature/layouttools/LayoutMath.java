/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2021, geheur and Bank Tag Layouts contributors
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
package com.ultimatebank.feature.layouttools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure layout calculations on core-style layouts: an int array of item IDs by bank position, 8 per row,
 * -1 for empty. Ported from Bank Tag Layouts by geheur (LayoutGenerator.zigzagLayout and Layout row helpers).
 */
final class LayoutMath
{
	static final int ROW = 8;
	private static final int EMPTY = -1;
	/** Furthest position displaced items may be moved to (the original's limit) */
	private static final int MAX_POSITION = 2000 / 38 * ROW;

	private LayoutMath()
	{
	}

	/**
	 * Lays out gear then inventory in a zigzag: down-up through each column of a pair of rows, 16 items per pair.
	 * Rune pouch runes follow in a plain row. Items already in the layout keep their position unless gear or
	 * inventory takes it, in which case they move to the first free positions after everything placed.
	 *
	 * @param equipped       worn item IDs in equipment slot order, -1 for empty slots
	 * @param inventory      inventory item IDs in slot order, -1 for empty slots
	 * @param runes          rune pouch runes, or null for none
	 * @param current        the tab's current layout
	 * @param duplicateLimit runs of the same inventory item up to this long get one position each; longer runs
	 *                       collapse to a single position. 0 or less means no duplicates at all.
	 */
	static int[] zigzag(int[] equipped, int[] inventory, int[] runes, int[] current, int duplicateLimit)
	{
		List<Integer> displaced = new ArrayList<>();
		Placement p = new Placement(new int[0], current, displaced);

		int next = p.place(toList(equipped), 0, true);

		List<Integer> inv = Arrays.stream(inventory).filter(id -> id != EMPTY).boxed().collect(Collectors.toList());
		inv = duplicateLimit <= 0 ? inv.stream().distinct().collect(Collectors.toList()) : limitDuplicates(inv, duplicateLimit);
		next = p.place(inv, next, true);

		if (runes != null)
		{
			next = p.place(toList(runes), next, false);
		}

		// Keep every other item from the current layout where it was, if that spot is still free
		for (int pos = 0; pos < current.length; pos++)
		{
			if (current[pos] != EMPTY && p.get(pos) == EMPTY)
			{
				p.set(pos, current[pos]);
			}
		}

		// Items pushed out by gear or inventory go to the first spots that were free in the current layout
		List<Integer> homeless = displaced.stream().filter(id -> !p.contains(id)).collect(Collectors.toList());
		for (int pos = next; !homeless.isEmpty() && pos < MAX_POSITION; pos++)
		{
			if (itemAt(current, pos) == EMPTY)
			{
				p.set(pos, homeless.remove(0));
			}
		}
		return p.layout;
	}

	/**
	 * Gives each run of identical consecutive items one position per item, unless the run is longer than the
	 * limit, in which case the run gets a single position.
	 */
	static List<Integer> limitDuplicates(List<Integer> items, int limit)
	{
		List<Integer> result = new ArrayList<>();
		int i = 0;
		while (i < items.size())
		{
			int itemId = items.get(i);
			int run = 0;
			while (i < items.size() && items.get(i) == itemId)
			{
				run++;
				i++;
			}
			int copies = run > limit ? 1 : run;
			for (int c = 0; c < copies; c++)
			{
				result.add(itemId);
			}
		}
		return result;
	}

	/**
	 * Position of the nth item in a zigzag: alternating between the top and bottom row of a pair, left to right.
	 */
	static int zigzagPosition(int n)
	{
		int rowPair = n / 16;
		int inPair = n % 16;
		return rowPair * 2 * ROW + (inPair % 2 == 0 ? 0 : ROW) + inPair / 2;
	}

	/**
	 * @return the layout with an empty row inserted at {@code row}, pushing everything from there down
	 */
	static int[] insertRow(int[] layout, int row)
	{
		int start = row * ROW;
		if (start >= layout.length)
		{
			return layout;
		}
		int[] result = new int[layout.length + ROW];
		Arrays.fill(result, EMPTY);
		System.arraycopy(layout, 0, result, 0, start);
		System.arraycopy(layout, start, result, start + ROW, layout.length - start);
		return result;
	}

	/**
	 * @return the layout with the (empty) row at {@code row} removed, pulling everything below up
	 */
	static int[] removeRow(int[] layout, int row)
	{
		int start = row * ROW;
		if (start >= layout.length || !isRowEmpty(layout, row))
		{
			return layout;
		}
		int end = Math.min(start + ROW, layout.length);
		int[] result = new int[layout.length - (end - start)];
		System.arraycopy(layout, 0, result, 0, start);
		System.arraycopy(layout, end, result, start, layout.length - end);
		return result;
	}

	static boolean isRowEmpty(int[] layout, int row)
	{
		for (int pos = row * ROW; pos < (row + 1) * ROW; pos++)
		{
			if (itemAt(layout, pos) != EMPTY)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return rows up to and including the last row holding an item
	 */
	static int rowCount(int[] layout)
	{
		for (int pos = layout.length - 1; pos >= 0; pos--)
		{
			if (layout[pos] != EMPTY)
			{
				return pos / ROW + 1;
			}
		}
		return 0;
	}

	private static int itemAt(int[] layout, int pos)
	{
		return pos < layout.length ? layout[pos] : EMPTY;
	}

	private static List<Integer> toList(int[] items)
	{
		return Arrays.stream(items).boxed().collect(Collectors.toList());
	}

	/**
	 * A layout being built, growing as items are placed.
	 */
	private static final class Placement
	{
		int[] layout;
		final int[] current;
		final List<Integer> displaced;

		Placement(int[] layout, int[] current, List<Integer> displaced)
		{
			this.layout = layout;
			this.current = current;
			this.displaced = displaced;
		}

		/**
		 * Places items from the nth position on (zigzag or row by row) and returns where the next group starts:
		 * the row (pair, for zigzag) after the last used position.
		 */
		int place(List<Integer> items, int n, boolean zigzag)
		{
			for (int itemId : items)
			{
				if (itemId == EMPTY)
				{
					continue;
				}
				int pos = zigzag ? zigzagPosition(n) : n;
				set(pos, itemId);
				if (itemAt(current, pos) != EMPTY)
				{
					displaced.add(current[pos]);
				}
				n++;
			}

			if (!items.isEmpty())
			{
				int last = lastUsed();
				if (last >= 0)
				{
					n = zigzag ? (last / 16 * 2 + 2) * ROW : (last / ROW + 1) * ROW;
				}
			}
			return n;
		}

		int get(int pos)
		{
			return itemAt(layout, pos);
		}

		void set(int pos, int itemId)
		{
			if (pos >= layout.length)
			{
				int oldLength = layout.length;
				layout = Arrays.copyOf(layout, pos + 1);
				Arrays.fill(layout, oldLength, layout.length, EMPTY);
			}
			layout[pos] = itemId;
		}

		boolean contains(int itemId)
		{
			return Arrays.stream(layout).anyMatch(id -> id == itemId);
		}

		int lastUsed()
		{
			for (int pos = layout.length - 1; pos >= 0; pos--)
			{
				if (layout[pos] != EMPTY)
				{
					return pos;
				}
			}
			return -1;
		}
	}
}

package com.ultimatebank.feature.storagesavings.marker;

import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * Where the corner triangle or icon goes within an item slot. Pure: no client access.
 */
public final class MarkerGeometry
{
	private MarkerGeometry()
	{
	}

	/**
	 * @return a right triangle of the given size filling the chosen corner of the slot
	 */
	public static Polygon triangle(Rectangle slot, MarkerStyle.Corner corner, int size)
	{
		int x = corner.isRight() ? slot.x + slot.width : slot.x;
		int y = corner.isBottom() ? slot.y + slot.height : slot.y;
		int dx = corner.isRight() ? -size : size;
		int dy = corner.isBottom() ? -size : size;
		return new Polygon(new int[]{x, x + dx, x}, new int[]{y, y, y + dy}, 3);
	}

	/**
	 * @return a box of the given size sitting in the chosen corner of the slot
	 */
	public static Rectangle cornerBox(Rectangle slot, MarkerStyle.Corner corner, int width, int height)
	{
		int x = corner.isRight() ? slot.x + slot.width - width : slot.x;
		int y = corner.isBottom() ? slot.y + slot.height - height : slot.y;
		return new Rectangle(x, y, width, height);
	}
}

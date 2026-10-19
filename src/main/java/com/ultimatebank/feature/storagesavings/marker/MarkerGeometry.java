package com.ultimatebank.feature.storagesavings.marker;

import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * Where the corner triangle goes within an item slot. Pure: no client access.
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
}

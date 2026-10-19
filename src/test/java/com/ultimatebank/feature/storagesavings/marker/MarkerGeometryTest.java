package com.ultimatebank.feature.storagesavings.marker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import org.junit.Test;

public class MarkerGeometryTest
{
	private static final Rectangle SLOT = new Rectangle(100, 200, 36, 32);

	@Test
	public void trianglesFillTheirCorner()
	{
		Polygon br = MarkerGeometry.triangle(SLOT, MarkerStyle.Corner.BOTTOM_RIGHT, 8);
		assertArrayEquals(new int[]{136, 128, 136}, br.xpoints);
		assertArrayEquals(new int[]{232, 232, 224}, br.ypoints);

		Polygon tl = MarkerGeometry.triangle(SLOT, MarkerStyle.Corner.TOP_LEFT, 8);
		assertArrayEquals(new int[]{100, 108, 100}, tl.xpoints);
		assertArrayEquals(new int[]{200, 200, 208}, tl.ypoints);
	}

	@Test
	public void iconsSitInsideTheirCorner()
	{
		assertEquals(new Point(126, 222), MarkerGeometry.iconOrigin(SLOT, MarkerStyle.Corner.BOTTOM_RIGHT, 10));
		assertEquals(new Point(100, 200), MarkerGeometry.iconOrigin(SLOT, MarkerStyle.Corner.TOP_LEFT, 10));
		assertEquals(new Point(126, 200), MarkerGeometry.iconOrigin(SLOT, MarkerStyle.Corner.TOP_RIGHT, 10));
		assertEquals(new Point(100, 222), MarkerGeometry.iconOrigin(SLOT, MarkerStyle.Corner.BOTTOM_LEFT, 10));
	}
}

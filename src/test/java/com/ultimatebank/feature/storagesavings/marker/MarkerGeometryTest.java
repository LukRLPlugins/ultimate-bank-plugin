package com.ultimatebank.feature.storagesavings.marker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
	public void boxesSitInTheirCorner()
	{
		assertEquals(new Rectangle(116, 212, 20, 20), MarkerGeometry.cornerBox(SLOT, MarkerStyle.Corner.BOTTOM_RIGHT, 20, 20));
		assertEquals(new Rectangle(100, 200, 20, 18), MarkerGeometry.cornerBox(SLOT, MarkerStyle.Corner.TOP_LEFT, 20, 18));
		assertEquals(new Rectangle(116, 200, 20, 18), MarkerGeometry.cornerBox(SLOT, MarkerStyle.Corner.TOP_RIGHT, 20, 18));
		assertEquals(new Rectangle(100, 214, 20, 18), MarkerGeometry.cornerBox(SLOT, MarkerStyle.Corner.BOTTOM_LEFT, 20, 18));
	}
}

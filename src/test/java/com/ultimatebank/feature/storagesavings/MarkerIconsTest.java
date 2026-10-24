package com.ultimatebank.feature.storagesavings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class MarkerIconsTest
{
	private static StorageSavingsState.LocationSavings location(String name, Integer... itemIds)
	{
		return new StorageSavingsState.LocationSavings(name, false, List.of(), List.of(itemIds), null, null, 0);
	}

	@Test
	public void eachItemTakesItsTopRankedStorage()
	{
		List<StorageSavingsState.LocationSavings> ranking = List.of(
			location("Big", 1, 2),
			location("Small", 2, 3),
			location("Unknown", 4));
		Map<Integer, Integer> icons = MarkerIcons.byItem(ranking,
			name -> name.equals("Big") ? 100 : name.equals("Small") ? 200 : -1);
		assertEquals(Map.of(1, 100, 2, 100, 3, 200), icons);
	}

	@Test
	public void fitCropsToVisiblePixels()
	{
		BufferedImage sprite = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		fill(sprite, 10, 8, 12, 6);
		BufferedImage icon = MarkerIcons.fit(sprite, 16);
		// Smaller than the box: cropped, never enlarged
		assertEquals(12, icon.getWidth());
		assertEquals(6, icon.getHeight());
	}

	@Test
	public void fitShrinksToTheBoxKeepingShape()
	{
		BufferedImage sprite = new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB);
		fill(sprite, 2, 6, 32, 16);
		BufferedImage icon = MarkerIcons.fit(sprite, 16);
		assertEquals(16, icon.getWidth());
		assertEquals(8, icon.getHeight());
		assertEquals(255, icon.getRGB(8, 4) >>> 24);
	}

	@Test
	public void blankSpriteHasNoIcon()
	{
		assertNull(MarkerIcons.fit(new BufferedImage(36, 32, BufferedImage.TYPE_INT_ARGB), 16));
	}

	private static void fill(BufferedImage image, int x, int y, int width, int height)
	{
		for (int dy = 0; dy < height; dy++)
		{
			for (int dx = 0; dx < width; dx++)
			{
				image.setRGB(x + dx, y + dy, Color.RED.getRGB());
			}
		}
	}
}

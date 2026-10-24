package com.ultimatebank.feature.storagesavings;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Picks and prepares the corner icon for each storable item: the sprite of the storage it would go in. Pure: no
 * client access.
 */
final class MarkerIcons
{
	private MarkerIcons()
	{
	}

	/**
	 * @param ranking storages by bank slots freed, most first
	 * @param iconForLocation the icon item for a storage's name, or -1 for none
	 * @return icon item ID by flagged item: the highest ranked storage that can hold it, as in the storable tab
	 */
	static Map<Integer, Integer> byItem(List<StorageSavingsState.LocationSavings> ranking, ToIntFunction<String> iconForLocation)
	{
		Map<Integer, Integer> icons = new HashMap<>();
		for (StorageSavingsState.LocationSavings location : ranking)
		{
			int icon = iconForLocation.applyAsInt(location.getName());
			if (icon == -1)
			{
				continue;
			}
			for (int itemId : location.getItemIds())
			{
				icons.putIfAbsent(itemId, icon);
			}
		}
		return Map.copyOf(icons);
	}

	/**
	 * Crops a sprite to its visible pixels and shrinks it to fit a square, averaging pixels so it stays sharp. It's
	 * never enlarged.
	 *
	 * @return the fitted icon, or null if the sprite has no visible pixels
	 */
	static BufferedImage fit(BufferedImage sprite, int size)
	{
		int minX = sprite.getWidth();
		int minY = sprite.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < sprite.getHeight(); y++)
		{
			for (int x = 0; x < sprite.getWidth(); x++)
			{
				if ((sprite.getRGB(x, y) >>> 24) != 0)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		if (maxX == -1)
		{
			return null;
		}

		BufferedImage cropped = sprite.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
		double scale = Math.min(1, Math.min((double) size / cropped.getWidth(), (double) size / cropped.getHeight()));
		int width = Math.max(1, (int) Math.round(cropped.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(cropped.getHeight() * scale));
		Image scaled = scale == 1 ? cropped : cropped.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);

		BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.drawImage(scaled, 0, 0, null);
		g.dispose();
		return icon;
	}
}

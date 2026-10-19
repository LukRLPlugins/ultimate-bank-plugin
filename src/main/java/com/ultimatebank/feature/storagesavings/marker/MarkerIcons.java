package com.ultimatebank.feature.storagesavings.marker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Preset corner icons, drawn in code (no image assets) in the marker colour with a dark outline. Cached, since the
 * overlay draws them every frame.
 */
public final class MarkerIcons
{
	private static final Color OUTLINE = new Color(0, 0, 0, 200);
	private static final Map<Key, BufferedImage> CACHE = new HashMap<>();

	private MarkerIcons()
	{
	}

	/**
	 * @param icon a preset (not {@link MarkerStyle.Icon#CUSTOM_ITEM})
	 */
	public static synchronized BufferedImage get(MarkerStyle.Icon icon, int size, Color color)
	{
		return CACHE.computeIfAbsent(new Key(icon, size, color), k -> draw(icon, size, color));
	}

	private static BufferedImage draw(MarkerStyle.Icon icon, int size, Color color)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// Design on a 10x10 grid
		float s = size / 10f;
		g.scale(s, s);
		java.awt.Shape shape;
		switch (icon)
		{
			case HOUSE:
				Path2D house = new Path2D.Float();
				house.moveTo(5, 0.8);
				house.lineTo(9.4, 4.8);
				house.lineTo(8, 4.8);
				house.lineTo(8, 9.2);
				house.lineTo(2, 9.2);
				house.lineTo(2, 4.8);
				house.lineTo(0.6, 4.8);
				house.closePath();
				shape = house;
				break;
			case CHEST:
				shape = new RoundRectangle2D.Float(0.8f, 2.5f, 8.4f, 6.7f, 2, 2);
				break;
			case STAR:
				Path2D star = new Path2D.Float();
				for (int i = 0; i < 10; i++)
				{
					double r = i % 2 == 0 ? 4.7 : 2;
					double a = Math.PI / 5 * i - Math.PI / 2;
					double x = 5 + r * Math.cos(a);
					double y = 5.2 + r * Math.sin(a);
					if (i == 0)
					{
						star.moveTo(x, y);
					}
					else
					{
						star.lineTo(x, y);
					}
				}
				star.closePath();
				shape = star;
				break;
			case EXCLAMATION:
				Path2D bang = new Path2D.Float();
				bang.append(new RoundRectangle2D.Float(3.6f, 0.6f, 2.8f, 6, 1.4f, 1.4f), false);
				bang.append(new Ellipse2D.Float(3.6f, 7, 2.8f, 2.8f), false);
				shape = bang;
				break;
			default:
				// ARROW: pointing up, "move this out"
				Path2D arrow = new Path2D.Float();
				arrow.moveTo(5, 0.6);
				arrow.lineTo(9.4, 5.2);
				arrow.lineTo(6.6, 5.2);
				arrow.lineTo(6.6, 9.4);
				arrow.lineTo(3.4, 9.4);
				arrow.lineTo(3.4, 5.2);
				arrow.lineTo(0.6, 5.2);
				arrow.closePath();
				shape = arrow;
				break;
		}
		g.setColor(color);
		g.fill(shape);
		g.setColor(OUTLINE);
		// One screen pixel wide, whatever the size
		g.setStroke(new BasicStroke(1f / s));
		g.draw(shape);
		if (icon == MarkerStyle.Icon.CHEST)
		{
			g.drawLine(1, 5, 9, 5);
		}
		g.dispose();
		return image;
	}

	private static final class Key
	{
		private final MarkerStyle.Icon icon;
		private final int size;
		private final int rgb;

		Key(MarkerStyle.Icon icon, int size, Color color)
		{
			this.icon = icon;
			this.size = size;
			this.rgb = color.getRGB();
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Key))
			{
				return false;
			}
			Key k = (Key) o;
			return icon == k.icon && size == k.size && rgb == k.rgb;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(icon, size, rgb);
		}
	}
}

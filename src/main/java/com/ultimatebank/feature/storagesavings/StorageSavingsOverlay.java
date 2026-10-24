package com.ultimatebank.feature.storagesavings;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.feature.storagesavings.marker.MarkerGeometry;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Marks flagged bank items in the configured style (item outline, box outline, corner triangle or corner icon), and
 * names where they can be stored on hover.
 */
@Singleton
class StorageSavingsOverlay extends WidgetItemOverlay
{
	private static final Color EDGE = Color.BLACK;
	/** Outline images kept; one per item, quantity, colour and thickness on screen */
	private static final int OUTLINE_CACHE_SIZE = 300;
	/** Behind the corner icon, so it reads over any item */
	private static final Color BADGE = new Color(0, 0, 0, 170);
	/** Space between the icon and its badge's edge */
	private static final int BADGE_PAD = 2;

	private final Client client;
	private final TooltipManager tooltipManager;
	private final UltimateBankConfig config;
	private final StorageSavingsState state;
	private final ItemManager itemManager;

	private final Map<String, BufferedImage> outlines = new LinkedHashMap<>(16, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest)
		{
			return size() > OUTLINE_CACHE_SIZE;
		}
	};
	/** Fitted corner icons by icon item, at {@link #iconSize}. Only touched on the client thread */
	private final Map<Integer, BufferedImage> icons = new HashMap<>();
	private final Set<Integer> loadingIcons = new HashSet<>();
	private int iconSize;

	@Inject
	StorageSavingsOverlay(Client client, TooltipManager tooltipManager, UltimateBankConfig config, StorageSavingsState state,
		ItemManager itemManager)
	{
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.config = config;
		this.state = state;
		this.itemManager = itemManager;
		showOnBank();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		if (!config.storageSavingsMarker() || itemWidget.getWidget().getParentId() != InterfaceID.Bankmain.ITEMS)
		{
			return;
		}

		List<String> locations = state.getFlagged().get(itemId);
		if (locations == null)
		{
			return;
		}

		Rectangle bounds = itemWidget.getCanvasBounds();
		Color color = config.storageSavingsMarkerColor();
		switch (config.storageSavingsMarkerStyle())
		{
			case ITEM_OUTLINE:
				drawItemOutline(graphics, itemId, itemWidget.getQuantity(), bounds, color);
				break;
			case BOX_OUTLINE:
				drawBox(graphics, bounds, color);
				break;
			case CORNER_ICON:
				drawIcon(graphics, itemId, bounds, color);
				break;
			default:
				drawTriangle(graphics, bounds, color);
				break;
		}

		Point mouse = client.getMouseCanvasPosition();
		if (bounds.contains(mouse.getX(), mouse.getY()))
		{
			tooltipManager.add(new Tooltip("Can be stored in:</br>" + String.join("</br>", locations)));
		}
	}

	private void drawTriangle(Graphics2D graphics, Rectangle bounds, Color color)
	{
		Polygon marker = MarkerGeometry.triangle(bounds, config.storageSavingsMarkerCorner(), config.storageSavingsMarkerSize());
		graphics.setColor(color);
		graphics.fill(marker);
		graphics.setColor(EDGE);
		graphics.draw(marker);
	}

	/**
	 * Draws the sprite of the storage the item would go in, on a dark badge edged in the marker colour. Falls back
	 * to the triangle until the sprite has loaded.
	 */
	private void drawIcon(Graphics2D graphics, int itemId, Rectangle bounds, Color color)
	{
		Integer iconItemId = state.get().getMarkerIcons().get(itemId);
		BufferedImage icon = iconItemId == null ? null : icon(iconItemId, config.storageSavingsMarkerIconSize());
		if (icon == null)
		{
			drawTriangle(graphics, bounds, color);
			return;
		}

		Rectangle badge = MarkerGeometry.cornerBox(bounds, config.storageSavingsMarkerCorner(),
			icon.getWidth() + BADGE_PAD * 2, icon.getHeight() + BADGE_PAD * 2);
		graphics.setColor(BADGE);
		graphics.fillRoundRect(badge.x, badge.y, badge.width - 1, badge.height - 1, 4, 4);
		graphics.setColor(color);
		graphics.drawRoundRect(badge.x, badge.y, badge.width - 1, badge.height - 1, 4, 4);
		graphics.drawImage(icon, badge.x + BADGE_PAD, badge.y + BADGE_PAD, null);
	}

	/**
	 * @return the fitted icon, or null while its sprite loads
	 */
	private BufferedImage icon(int iconItemId, int size)
	{
		if (size != iconSize)
		{
			icons.clear();
			iconSize = size;
		}
		BufferedImage icon = icons.get(iconItemId);
		if (icon == null && loadingIcons.add(iconItemId))
		{
			AsyncBufferedImage sprite = itemManager.getImage(iconItemId);
			sprite.onLoaded(() ->
			{
				loadingIcons.remove(iconItemId);
				BufferedImage fitted = MarkerIcons.fit(sprite, size);
				if (fitted != null && size == iconSize)
				{
					icons.put(iconItemId, fitted);
				}
			});
		}
		return icon;
	}

	private void drawBox(Graphics2D graphics, Rectangle bounds, Color color)
	{
		int t = config.storageSavingsMarkerThickness();
		Stroke previous = graphics.getStroke();
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(t));
		// Inset by half the stroke so the box stays inside the slot
		graphics.drawRect(bounds.x + t / 2, bounds.y + t / 2, bounds.width - t, bounds.height - t);
		graphics.setStroke(previous);
	}

	private void drawItemOutline(Graphics2D graphics, int itemId, int quantity, Rectangle bounds, Color color)
	{
		int thickness = config.storageSavingsMarkerThickness();
		String key = itemId + ":" + quantity + ":" + color.getRGB() + ":" + thickness;
		BufferedImage outline = outlines.computeIfAbsent(key, k -> thicken(itemManager.getItemOutline(itemId, quantity, color), thickness));
		int pad = thickness - 1;
		graphics.drawImage(outline, bounds.x - pad, bounds.y - pad, null);
	}

	/**
	 * @return the 1px outline stamped at every offset up to thickness - 1, on a canvas padded to fit
	 */
	private static BufferedImage thicken(BufferedImage outline, int thickness)
	{
		if (thickness <= 1)
		{
			return outline;
		}
		int pad = thickness - 1;
		BufferedImage thick = new BufferedImage(outline.getWidth() + pad * 2, outline.getHeight() + pad * 2,
			BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = thick.createGraphics();
		for (int dx = -pad; dx <= pad; dx++)
		{
			for (int dy = -pad; dy <= pad; dy++)
			{
				g.drawImage(outline, pad + dx, pad + dy, null);
			}
		}
		g.dispose();
		return thick;
	}
}

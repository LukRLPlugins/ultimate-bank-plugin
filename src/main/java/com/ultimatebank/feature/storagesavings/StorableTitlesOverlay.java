package com.ultimatebank.feature.storagesavings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws each container's name and a divider line in the blank title rows of the grouped storable tab. Layouts can't
 * hold text, so titles are an overlay (approach from Bank Skill Sorting by Frailrain).
 */
@Singleton
class StorableTitlesOverlay extends Overlay
{
	private static final int ROW_HEIGHT = BankTagsPlugin.BANK_ITEM_HEIGHT + BankTagsPlugin.BANK_ITEM_Y_PADDING;
	private static final Color SHADOW = new Color(0, 0, 0, 170);
	private static final Color TITLE = new Color(255, 255, 255, 230);
	private static final Color LINE = new Color(160, 160, 160, 170);

	private final Client client;
	private final StorageSavingsTagTab tagTab;

	@Inject
	StorableTitlesOverlay(Client client, StorageSavingsTagTab tagTab)
	{
		this.client = client;
		this.tagTab = tagTab;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Map<Integer, String> titles = tagTab.getTitleRows();
		if (titles.isEmpty())
		{
			return null;
		}

		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null || container.isHidden())
		{
			return null;
		}

		Rectangle clip = container.getBounds();
		Point origin = container.getCanvasLocation();
		int left = origin.getX() + BankTagsPlugin.BANK_ITEM_START_X;
		int right = clip.x + clip.width - 12;
		graphics.setFont(FontManager.getRunescapeBoldFont());

		for (Map.Entry<Integer, String> entry : titles.entrySet())
		{
			int rowTop = origin.getY() + BankTagsPlugin.BANK_ITEM_START_Y + entry.getKey() * ROW_HEIGHT - container.getScrollY();
			int lineY = rowTop + ROW_HEIGHT / 2;
			// Skip rows scrolled out of the bank's visible area
			if (lineY < clip.y + 6 || lineY > clip.y + clip.height - 4)
			{
				continue;
			}

			String title = entry.getValue();
			int baseline = lineY + graphics.getFontMetrics().getAscent() / 2 - 1;
			graphics.setColor(SHADOW);
			graphics.drawString(title, left + 1, baseline + 1);
			graphics.setColor(TITLE);
			graphics.drawString(title, left, baseline);

			int lineStart = left + graphics.getFontMetrics().stringWidth(title) + 8;
			if (right > lineStart)
			{
				graphics.setColor(LINE);
				graphics.drawLine(lineStart, lineY, right, lineY);
			}
		}
		return null;
	}
}

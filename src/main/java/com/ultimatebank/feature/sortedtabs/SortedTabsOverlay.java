package com.ultimatebank.feature.sortedtabs;

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
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws section titles and divider lines in the blank title rows of the sorted tab being viewed. Layouts can't hold
 * text, so titles are an overlay (as in Bank Skill Sorting). Sub-section titles are smaller and indented.
 */
@Singleton
class SortedTabsOverlay extends Overlay
{
	private static final int ROW_HEIGHT = BankTagsPlugin.BANK_ITEM_HEIGHT + BankTagsPlugin.BANK_ITEM_Y_PADDING;
	private static final Color SHADOW = new Color(0, 0, 0, 170);
	private static final Color TITLE = new Color(255, 255, 255, 230);
	private static final Color SUB_TITLE = new Color(255, 200, 120, 220);
	private static final Color LINE = new Color(160, 160, 160, 170);
	private static final int SUB_INDENT = 12;

	private final Client client;
	private final BankTagsService bankTagsService;
	private final SortedTabsState state;

	@Inject
	SortedTabsOverlay(Client client, BankTagsService bankTagsService, SortedTabsState state)
	{
		this.client = client;
		this.bankTagsService = bankTagsService;
		this.state = state;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Map<Integer, SortedLayoutBuilder.Header> headers = state.headersFor(bankTagsService.getActiveTag());
		if (headers.isEmpty())
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

		for (Map.Entry<Integer, SortedLayoutBuilder.Header> entry : headers.entrySet())
		{
			SortedLayoutBuilder.Header header = entry.getValue();
			int rowTop = origin.getY() + BankTagsPlugin.BANK_ITEM_START_Y + entry.getKey() * ROW_HEIGHT - container.getScrollY();
			int lineY = rowTop + ROW_HEIGHT / 2;
			if (lineY < clip.y + 6 || lineY > clip.y + clip.height - 4)
			{
				continue;
			}

			boolean sub = header.getLevel() > 1;
			graphics.setFont(sub ? FontManager.getRunescapeSmallFont() : FontManager.getRunescapeBoldFont());
			int x = sub ? left + SUB_INDENT : left;
			int baseline = lineY + graphics.getFontMetrics().getAscent() / 2 - 1;

			graphics.setColor(SHADOW);
			graphics.drawString(header.getTitle(), x + 1, baseline + 1);
			graphics.setColor(sub ? SUB_TITLE : TITLE);
			graphics.drawString(header.getTitle(), x, baseline);

			int lineStart = x + graphics.getFontMetrics().stringWidth(header.getTitle()) + 8;
			if (right > lineStart)
			{
				graphics.setColor(LINE);
				graphics.drawLine(lineStart, lineY, right, lineY);
			}
		}
		return null;
	}
}

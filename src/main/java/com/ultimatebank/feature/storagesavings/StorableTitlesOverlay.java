package com.ultimatebank.feature.storagesavings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
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
 * hold text, so titles are an overlay (approach from Bank Skill Sorting by Frailrain). Also remembers where each title
 * row was drawn, so a click on it can focus that group.
 */
@Singleton
class StorableTitlesOverlay extends Overlay
{
	private static final int ROW_HEIGHT = BankTagsPlugin.BANK_ITEM_HEIGHT + BankTagsPlugin.BANK_ITEM_Y_PADDING;
	private static final Color SHADOW = new Color(0, 0, 0, 170);
	private static final Color TITLE = new Color(255, 255, 255, 230);
	private static final Color LINE = new Color(160, 160, 160, 170);
	private static final Color HOVER = new Color(255, 200, 0, 240);
	private static final Color HINT = new Color(200, 200, 200, 200);

	private final Client client;
	private final StorageSavingsTagTab tagTab;
	/** Title rows drawn last frame, for clicks */
	private volatile List<TitleRow> drawnRows = Collections.emptyList();

	@Value
	private static class TitleRow
	{
		Rectangle bounds;
		String title;
	}

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
		drawnRows = Collections.emptyList();
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
		Point mouse = client.getMouseCanvasPosition();
		String focus = tagTab.getFocus();
		List<TitleRow> rows = new ArrayList<>();

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
			Rectangle bounds = new Rectangle(clip.x, rowTop, clip.width, ROW_HEIGHT).intersection(clip);
			rows.add(new TitleRow(bounds, title));
			boolean hovered = mouse != null && bounds.contains(mouse.getX(), mouse.getY());

			int baseline = lineY + graphics.getFontMetrics().getAscent() / 2 - 1;
			graphics.setColor(SHADOW);
			graphics.drawString(title, left + 1, baseline + 1);
			graphics.setColor(hovered ? HOVER : TITLE);
			graphics.drawString(title, left, baseline);
			int textEnd = left + graphics.getFontMetrics().stringWidth(title);

			String hint = focus != null ? "click to show all" : hovered ? "click to show only this" : null;
			if (hint != null)
			{
				graphics.setFont(FontManager.getRunescapeSmallFont());
				graphics.setColor(SHADOW);
				graphics.drawString(hint, textEnd + 9, baseline + 1);
				graphics.setColor(HINT);
				graphics.drawString(hint, textEnd + 8, baseline);
				textEnd += 8 + graphics.getFontMetrics().stringWidth(hint);
				graphics.setFont(FontManager.getRunescapeBoldFont());
			}

			int lineStart = textEnd + 8;
			if (right > lineStart)
			{
				graphics.setColor(LINE);
				graphics.drawLine(lineStart, lineY, right, lineY);
			}
		}
		drawnRows = rows;
		return null;
	}

	/**
	 * @return the title of the group whose title row is at this canvas point, or null
	 */
	String titleAt(java.awt.Point point)
	{
		for (TitleRow row : drawnRows)
		{
			if (row.getBounds().contains(point))
			{
				return row.getTitle();
			}
		}
		return null;
	}
}

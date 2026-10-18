package com.ultimatebank.feature.storagesavings;

import com.ultimatebank.UltimateBankConfig;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

/**
 * Marks flagged bank items with a corner triangle, and names where they can be stored on hover.
 */
@Singleton
class StorageSavingsOverlay extends WidgetItemOverlay
{
	private static final int MARKER_SIZE = 8;

	private final Client client;
	private final TooltipManager tooltipManager;
	private final UltimateBankConfig config;
	private final StorageSavingsState state;

	@Inject
	StorageSavingsOverlay(Client client, TooltipManager tooltipManager, UltimateBankConfig config, StorageSavingsState state)
	{
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.config = config;
		this.state = state;
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
		int right = bounds.x + bounds.width;
		int bottom = bounds.y + bounds.height;
		Polygon marker = new Polygon(
			new int[]{right, right, right - MARKER_SIZE},
			new int[]{bottom - MARKER_SIZE, bottom, bottom},
			3);
		graphics.setColor(config.storageSavingsMarkerColor());
		graphics.fill(marker);
		graphics.setColor(Color.BLACK);
		graphics.draw(marker);

		Point mouse = client.getMouseCanvasPosition();
		if (bounds.contains(mouse.getX(), mouse.getY()))
		{
			tooltipManager.add(new Tooltip("Can be stored in:</br>" + String.join("</br>", locations)));
		}
	}
}

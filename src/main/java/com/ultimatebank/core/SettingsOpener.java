package com.ultimatebank.core;

import com.ultimatebank.UltimateBankPlugin;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.MenuAction;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;

/**
 * Opens the plugin's settings in the RuneLite configuration panel, e.g. from a feature's sidebar panel.
 * There's no direct API for this; the core Configuration plugin opens a plugin's settings when one of its overlays'
 * "Configure" menu entries is clicked, so we post that event for an overlay belonging to our plugin.
 */
@Singleton
public class SettingsOpener
{
	private static final OverlayMenuEntry CONFIGURE = new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "Ultimate Bank");

	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final Overlay owner;

	@Inject
	SettingsOpener(ClientThread clientThread, EventBus eventBus, UltimateBankPlugin plugin)
	{
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		// Never added to the overlay manager; it only tells the Configuration plugin whose settings to open
		this.owner = new Overlay(plugin)
		{
			@Override
			public Dimension render(Graphics2D graphics)
			{
				return null;
			}
		};
	}

	/**
	 * Opens Ultimate Bank's settings. Safe to call from any thread.
	 */
	public void open()
	{
		// Overlay menu clicks are normally posted on the client thread
		clientThread.invokeLater(() -> eventBus.post(new OverlayMenuClicked(CONFIGURE, owner)));
	}
}

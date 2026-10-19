package com.ultimatebank.feature.wheresmystuff;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.snapshots.Snapshot;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import com.ultimatebank.core.snapshots.StorageSnapshots;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/**
 * A sidebar search across the bank and POH costume room storage, from saved snapshots, with values and what changed
 * since the previous snapshot. Ported from Where's My Stuff by Wozak-1; see docs/features/wheres-my-stuff.md.
 */
@Singleton
public class WheresMyStuffFeature implements BankFeature
{
	private static final String KEY_PREFIX = "wheresMyStuff";

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private UltimateBankConfig config;

	@Inject
	private StorageSnapshots snapshots;

	private final Runnable snapshotListener = this::refresh;
	private WheresMyStuffPanel panel;
	private NavigationButton navButton;

	@Override
	public boolean isEnabled(UltimateBankConfig config)
	{
		return config.wheresMyStuffEnabled();
	}

	@Override
	public void startUp()
	{
		panel = new WheresMyStuffPanel(itemManager::getImage);
		navButton = NavigationButton.builder()
			.tooltip("Where's my stuff")
			.icon(createIcon())
			.priority(9)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		snapshots.addListener(snapshotListener);
		refresh();
	}

	@Override
	public void shutDown()
	{
		snapshots.removeListener(snapshotListener);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (UltimateBankConfig.GROUP.equals(event.getGroup()) && event.getKey().startsWith(KEY_PREFIX))
		{
			clientThread.invokeLater(this::refresh);
		}
	}

	/**
	 * Rebuilds the rows from the latest snapshots. Client thread (item names and prices).
	 */
	private void refresh()
	{
		WheresMyStuffPanel target = panel;
		if (target == null)
		{
			return;
		}

		List<StuffRow> rows = new ArrayList<>();
		Map<SnapshotLocation, Long> seenAt = new EnumMap<>(SnapshotLocation.class);
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Snapshot latest = snapshots.latest(location);
			if (latest == null)
			{
				continue;
			}
			seenAt.put(location, latest.getTakenAt());

			Snapshot previous = snapshots.previous(location);
			Map<Integer, Integer> changes = SnapshotDiff.changes(previous == null ? null : previous.getQuantities(),
				latest.getQuantities());
			Set<Integer> ids = new HashSet<>(latest.getQuantities().keySet());
			// Items that left this location still show under "Recently changed"
			ids.addAll(changes.keySet());
			for (int itemId : ids)
			{
				ItemComposition item = itemManager.getItemComposition(itemId);
				long price = itemManager.getItemPrice(itemId);
				rows.add(new StuffRow(itemId, item.getName(), latest.getQuantities().getOrDefault(itemId, 0),
					price > 0 ? price : item.getHaPrice(), location, changes.getOrDefault(itemId, 0)));
			}
		}

		int warn = config.wheresMyStuffWarnHours();
		int stale = config.wheresMyStuffStaleHours();
		SwingUtilities.invokeLater(() -> target.update(rows, seenAt, warn, stale));
	}

	/**
	 * A small magnifying glass, drawn rather than shipped as a PNG.
	 */
	private static BufferedImage createIcon()
	{
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = icon.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(new Color(220, 220, 220));
		g.setStroke(new BasicStroke(2f));
		g.drawOval(2, 2, 8, 8);
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(10, 10, 14, 14);
		g.dispose();
		return icon;
	}
}

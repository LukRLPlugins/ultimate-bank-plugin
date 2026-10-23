package com.ultimatebank.feature.banksearch;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

/**
 * A sidebar panel of the bank and POH costume room storage contents, grouped by location and searchable, plus what
 * each visit took out or put in. Built from the saved snapshots. Ported from Where's My Stuff by Wozak-1; see docs/features/wheres-my-stuff.md.
 */
@Singleton
public class BankSearchFeature implements BankFeature
{
	private static final String KEY_PREFIX = "bankSearch";

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private UltimateBankConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private StorageSnapshots snapshots;

	private final Consumer<SnapshotLocation> snapshotListener = location -> refresh();
	private BankSearchPanel panel;
	private NavigationButton navButton;

	@Override
	public boolean isEnabled(UltimateBankConfig config)
	{
		return config.bankSearchEnabled();
	}

	@Override
	public void startUp()
	{
		panel = new BankSearchPanel(itemManager::getImage, config.bankSearchValueType(),
			type -> configManager.setConfiguration(UltimateBankConfig.GROUP, "bankSearchValueType", type));
		navButton = NavigationButton.builder()
			.tooltip("Ultimate Bank Search")
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
		BankSearchPanel target = panel;
		if (target == null)
		{
			return;
		}

		ValueType valueType = config.bankSearchValueType();
		List<StuffRow> items = new ArrayList<>();
		Map<SnapshotLocation, Long> seenAt = new EnumMap<>(SnapshotLocation.class);
		Map<SnapshotLocation, List<Snapshot>> histories = new EnumMap<>(SnapshotLocation.class);
		Map<Integer, StuffRow> info = new HashMap<>();
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Snapshot latest = snapshots.latest(location);
			if (latest == null)
			{
				continue;
			}
			seenAt.put(location, latest.getTakenAt());
			histories.put(location, snapshots.history(location));
			latest.getQuantities().forEach((itemId, quantity) ->
				items.add(row(info, valueType, itemId, quantity, location)));
		}

		List<StuffVisit> visits = new ArrayList<>();
		for (VisitLog.Visit visit : VisitLog.build(histories))
		{
			List<StuffRow> rows = new ArrayList<>();
			visit.getChanges().forEach((itemId, change) -> rows.add(row(info, valueType, itemId, change, visit.getLocation())));
			visits.add(new StuffVisit(visit.getLocation(), visit.getTakenAt(), rows));
		}

		int warn = config.bankSearchWarnHours();
		int stale = config.bankSearchStaleHours();
		SwingUtilities.invokeLater(() -> target.update(items, visits, seenAt, warn, stale, valueType));
	}

	/**
	 * @param info name and price per item, looked up once per refresh
	 */
	private StuffRow row(Map<Integer, StuffRow> info, ValueType valueType, int itemId, int quantity,
		SnapshotLocation location)
	{
		StuffRow known = info.computeIfAbsent(itemId, id ->
		{
			ItemComposition item = itemManager.getItemComposition(id);
			long price = valueType.unitPrice(id, itemManager.getItemPrice(id), item.getHaPrice());
			return new StuffRow(id, item.getName(), 0, price, location);
		});
		return new StuffRow(itemId, known.getName(), quantity, known.getUnitPrice(), location);
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

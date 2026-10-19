package com.ultimatebank.feature.wheresmystuff;

import com.ultimatebank.core.snapshots.Snapshot;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

/**
 * Search across the bank and POH storage snapshots, with value and recent changes. Swing thread only.
 */
class WheresMyStuffPanel extends PluginPanel
{
	private static final Color WARN = new Color(255, 152, 31);
	private static final Color STALE = new Color(230, 70, 70);
	private static final Color GAIN = new Color(80, 200, 80);
	private static final Color LOSS = new Color(230, 90, 90);
	/** Most rows the list shows at once; search narrows it down */
	private static final int MAX_ROWS = 500;

	enum Sort
	{
		NAME("Name"),
		VALUE("Value"),
		CHANGED("Recently changed");

		private final String label;

		Sort(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private final IntFunction<AsyncBufferedImage> images;
	private final IconTextField search = new IconTextField();
	private final JComboBox<Sort> sort = new JComboBox<>(Sort.values());
	private final JPanel freshness = new JPanel(new GridLayout(0, 1));
	private final JLabel status = new JLabel();
	private final JList<StuffRow> list = new JList<>();
	private final Map<Integer, ImageIcon> icons = new HashMap<>();

	private List<StuffRow> rows = Collections.emptyList();
	private Map<SnapshotLocation, Long> seenAt = Collections.emptyMap();
	private int warnHours = 24;
	private int staleHours = 168;

	/**
	 * @param images item icon loader (ItemManager::getImage)
	 */
	WheresMyStuffPanel(IntFunction<AsyncBufferedImage> images)
	{
		super(false);
		this.images = images;
		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel(new BorderLayout(0, 6));
		JLabel title = new JLabel("Where's my stuff");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		top.add(title, BorderLayout.NORTH);

		JPanel controls = new JPanel(new BorderLayout(0, 4));
		search.setIcon(IconTextField.Icon.SEARCH);
		search.setToolTipText("Search by item name or location");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				refreshList();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				refreshList();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				refreshList();
			}
		});
		search.addClearListener(this::refreshList);
		controls.add(search, BorderLayout.NORTH);
		sort.setToolTipText("Sort by");
		sort.addActionListener(e -> refreshList());
		controls.add(sort, BorderLayout.CENTER);
		controls.add(freshness, BorderLayout.SOUTH);
		top.add(controls, BorderLayout.CENTER);
		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		status.setFont(FontManager.getRunescapeSmallFont());
		top.add(status, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);

		list.setCellRenderer(new RowRenderer());
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JScrollPane scroll = new JScrollPane(list);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		add(scroll, BorderLayout.CENTER);

		update(Collections.emptyList(), Collections.emptyMap(), warnHours, staleHours);
	}

	void update(List<StuffRow> rows, Map<SnapshotLocation, Long> seenAt, int warnHours, int staleHours)
	{
		this.rows = rows;
		this.seenAt = seenAt;
		this.warnHours = warnHours;
		this.staleHours = staleHours;
		rebuildFreshness();
		refreshList();
	}

	private void rebuildFreshness()
	{
		freshness.removeAll();
		if (seenAt.isEmpty())
		{
			freshness.add(note("<html>Open your bank and each POH storage once to record them.</html>", ColorScheme.LIGHT_GRAY_COLOR));
		}
		for (SnapshotLocation location : SnapshotLocation.values())
		{
			Long at = seenAt.get(location);
			if (at == null)
			{
				continue;
			}
			long hours = (System.currentTimeMillis() - at) / 3_600_000;
			Color color = hours >= staleHours ? STALE : hours >= warnHours ? WARN : ColorScheme.LIGHT_GRAY_COLOR;
			freshness.add(note(location.getDisplayName() + ": seen " + Snapshot.ago(at), color));
		}
		freshness.revalidate();
	}

	private static JLabel note(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	private void refreshList()
	{
		String query = search.getText().trim().toLowerCase(Locale.ROOT);
		Sort by = (Sort) sort.getSelectedItem();

		List<StuffRow> shown = rows.stream()
			.filter(r -> query.isEmpty()
				|| r.getName().toLowerCase(Locale.ROOT).contains(query)
				|| r.getLocation().getDisplayName().toLowerCase(Locale.ROOT).contains(query))
			.filter(r -> by == Sort.CHANGED ? r.getChange() != 0 : r.getQuantity() > 0)
			.sorted(comparator(by))
			.collect(Collectors.toList());

		status.setText(shown.size() + (shown.size() == 1 ? " item" : " items")
			+ (shown.size() > MAX_ROWS ? " (showing " + MAX_ROWS + ", search to narrow)" : "")
			+ (by == Sort.CHANGED ? " changed since the previous snapshot" : ""));
		list.setListData(shown.subList(0, Math.min(shown.size(), MAX_ROWS)).toArray(new StuffRow[0]));
	}

	private static Comparator<StuffRow> comparator(Sort by)
	{
		Comparator<StuffRow> byName = Comparator.comparing(StuffRow::getName, String.CASE_INSENSITIVE_ORDER);
		switch (by)
		{
			case VALUE:
				return Comparator.comparingLong(StuffRow::totalValue).reversed().thenComparing(byName);
			case CHANGED:
				return Comparator.comparingLong((StuffRow r) -> Math.abs(r.getChange() * Math.max(1, r.getUnitPrice())))
					.reversed().thenComparing(byName);
			default:
				return byName.thenComparing(r -> r.getLocation().ordinal());
		}
	}

	private ImageIcon icon(int itemId)
	{
		return icons.computeIfAbsent(itemId, id ->
		{
			AsyncBufferedImage image = images.apply(id);
			ImageIcon icon = new ImageIcon(image);
			image.onLoaded(() ->
			{
				icon.setImage(image);
				list.repaint();
			});
			return icon;
		});
	}

	private class RowRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
			StuffRow row = (StuffRow) value;
			String change = "";
			if (row.getChange() != 0)
			{
				Color c = row.getChange() > 0 ? GAIN : LOSS;
				change = String.format(" <font color='#%02x%02x%02x'>%s%s</font>", c.getRed(), c.getGreen(), c.getBlue(),
					row.getChange() > 0 ? "+" : "", QuantityFormatter.formatNumber(row.getChange()));
			}
			label.setText("<html>" + row.getName() + " x " + QuantityFormatter.quantityToStackSize(row.getQuantity()) + change
				+ "<br><font color='#a5a5a5'>" + row.getLocation().getDisplayName()
				+ (row.getUnitPrice() > 0 ? " · " + QuantityFormatter.quantityToStackSize(row.totalValue()) + " gp" : "")
				+ "</font></html>");
			label.setIcon(icon(row.getItemId()));
			label.setBorder(new EmptyBorder(2, 2, 2, 2));
			if (!selected)
			{
				label.setBackground(index % 2 == 0 ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			}
			return label;
		}
	}
}

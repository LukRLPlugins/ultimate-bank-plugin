package com.ultimatebank.feature.banksearch;

import com.ultimatebank.core.snapshots.Snapshot;
import com.ultimatebank.core.snapshots.SnapshotLocation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

/**
 * Bank and POH storage contents grouped by location, and a history of what each visit changed. Searchable.
 * Must only be touched on the Swing thread.
 */
class BankSearchPanel extends PluginPanel
{
	private static final Color WARN = new Color(255, 152, 31);
	private static final Color STALE = new Color(230, 70, 70);
	private static final Color GAIN = new Color(80, 200, 80);
	private static final Color LOSS = new Color(230, 90, 90);
	/** Most rows one section or visit shows; search narrows it down */
	private static final int MAX_ROWS = 200;
	private static final int ROW_HEIGHT = 36;

	private enum View
	{
		ITEMS, HISTORY
	}

	private final IntFunction<AsyncBufferedImage> images;
	private final IconTextField search = new IconTextField();
	private final JComboBox<StuffGroups.Sort> sort = new JComboBox<>(StuffGroups.Sort.values());
	private final JComboBox<ValueType> valueType = new JComboBox<>(ValueType.values());
	private final JPanel options = new JPanel(new GridLayout(1, 2, 6, 0));
	/** Set while the value type box is updated from settings, so that isn't saved back */
	private boolean settingValueType;
	private final JPanel content = new JPanel();
	private final Map<Integer, ImageIcon> icons = new HashMap<>();
	/** Sections the user has opened, kept across updates */
	private final Set<SnapshotLocation> expanded = EnumSet.noneOf(SnapshotLocation.class);

	private View view = View.ITEMS;
	private List<StuffRow> items = Collections.emptyList();
	/** Value of everything recorded, for each item's share */
	private long grandTotal;
	private List<StuffVisit> visits = Collections.emptyList();
	private Map<SnapshotLocation, Long> seenAt = Collections.emptyMap();
	private int warnHours = 24;
	private int staleHours = 168;

	/**
	 * @param images         item icon loader (ItemManager::getImage)
	 * @param onValueType    saves a value type picked in the panel
	 */
	BankSearchPanel(IntFunction<AsyncBufferedImage> images, ValueType initialValueType, Consumer<ValueType> onValueType)
	{
		super(false);
		this.images = images;
		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		JLabel title = new JLabel("Ultimate Bank Search");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		top.add(leftAligned(title));

		search.setIcon(IconTextField.Icon.SEARCH);
		search.setToolTipText("Search by item name or location");
		search.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild();
			}
		});
		search.addClearListener(this::rebuild);
		top.add(leftAligned(search));

		MaterialTabGroup tabs = new MaterialTabGroup();
		tabs.setLayout(new GridLayout(1, 2));
		MaterialTab itemsTab = tab("Items", tabs, View.ITEMS);
		tab("History", tabs, View.HISTORY);
		tabs.select(itemsTab);
		top.add(leftAligned(tabs));

		sort.setToolTipText("Sort items by");
		sort.addActionListener(e -> rebuild());
		valueType.setSelectedItem(initialValueType);
		valueType.setToolTipText("Value items at their GE price or high alch value");
		valueType.addActionListener(e ->
		{
			if (!settingValueType)
			{
				onValueType.accept((ValueType) valueType.getSelectedItem());
			}
		});
		options.setOpaque(false);
		options.add(sort);
		options.add(valueType);
		top.add(leftAligned(options));
		add(top, BorderLayout.NORTH);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		JPanel holder = new WidthTrackingPanel();
		holder.add(content, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(holder);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(ROW_HEIGHT);
		add(scroll, BorderLayout.CENTER);

		rebuild();
	}

	void update(List<StuffRow> items, List<StuffVisit> visits, Map<SnapshotLocation, Long> seenAt, int warnHours,
		int staleHours, ValueType valueType)
	{
		this.items = items;
		grandTotal = StuffGroups.totalValue(items);
		settingValueType = true;
		this.valueType.setSelectedItem(valueType);
		settingValueType = false;
		this.visits = visits;
		this.seenAt = seenAt;
		this.warnHours = warnHours;
		this.staleHours = staleHours;
		rebuild();
	}

	private MaterialTab tab(String name, MaterialTabGroup tabs, View tabView)
	{
		MaterialTab tab = new MaterialTab(name, tabs, null);
		tab.setHorizontalAlignment(SwingConstants.CENTER);
		tab.setOnSelectEvent(() ->
		{
			view = tabView;
			rebuild();
			return true;
		});
		tabs.addTab(tab);
		return tab;
	}

	private void rebuild()
	{
		content.removeAll();
		sort.setEnabled(view == View.ITEMS);
		String query = search.getText().trim();
		if (view == View.ITEMS)
		{
			buildItems(query);
		}
		else
		{
			buildHistory(query);
		}
		content.revalidate();
		content.repaint();
	}

	private void buildItems(String query)
	{
		if (seenAt.isEmpty())
		{
			content.add(note("Open your bank and each POH storage once to record them."));
			return;
		}

		List<StuffGroups.Group> groups = StuffGroups.group(items, query, (StuffGroups.Sort) sort.getSelectedItem());
		if (groups.isEmpty())
		{
			content.add(note("No matches."));
		}
		else
		{
			content.add(totalCard(groups, query));
		}
		for (StuffGroups.Group group : groups)
		{
			SnapshotLocation location = group.getLocation();
			int count = group.getRows().size();
			JPanel card = card();
			JPanel heading = heading(location.getDisplayName(), location.isPoh() ? ColorScheme.BRAND_ORANGE : Color.WHITE,
				count + (count == 1 ? " item" : " items") + " · " + gp(group.getTotalValue()), ColorScheme.LIGHT_GRAY_COLOR);
			Long at = seenAt.get(location);
			if (at != null)
			{
				heading.add(small("seen " + Snapshot.ago(at), freshness(at)), BorderLayout.SOUTH);
			}
			card.add(heading, BorderLayout.NORTH);

			// A search opens every section it matches
			boolean open = !query.isEmpty() || expanded.contains(location);
			if (open)
			{
				card.add(rows(group.getRows(), false), BorderLayout.CENTER);
			}
			heading.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			heading.setToolTipText(open && query.isEmpty() ? "Click to close" : "Click to open");
			heading.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (!expanded.remove(location))
					{
						expanded.add(location);
					}
					rebuild();
				}
			});
			content.add(card);
		}
	}

	/**
	 * Everything recorded (or everything matching the search): its value and how many item stacks.
	 */
	private JPanel totalCard(List<StuffGroups.Group> groups, String query)
	{
		long value = groups.stream().mapToLong(StuffGroups.Group::getTotalValue).sum();
		int stacks = groups.stream().mapToInt(g -> g.getRows().size()).sum();
		JPanel card = card();
		card.add(heading(query.isEmpty() ? "Total" : "Matching", Color.WHITE,
			stacks + (stacks == 1 ? " item" : " items") + " · " + gp(value), Color.WHITE), BorderLayout.NORTH);
		String share = query.isEmpty() ? null : StuffGroups.share(value, grandTotal);
		card.add(small(groups.size() + (groups.size() == 1 ? " location" : " locations") + ", at "
			+ this.valueType.getSelectedItem() + (share == null ? "" : " · " + share + " of everything"),
			ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.SOUTH);
		return card;
	}

	private void buildHistory(String query)
	{
		String q = query.toLowerCase(Locale.ROOT);
		boolean any = false;
		for (StuffVisit visit : visits)
		{
			boolean locationMatches = visit.getLocation().getDisplayName().toLowerCase(Locale.ROOT).contains(q);
			List<StuffRow> rows = visit.getRows().stream()
				.filter(r -> q.isEmpty() || locationMatches || r.getName().toLowerCase(Locale.ROOT).contains(q))
				.sorted(StuffGroups.comparator(StuffGroups.Sort.VALUE))
				.collect(Collectors.toList());
			if (rows.isEmpty())
			{
				continue;
			}
			any = true;

			long net = visit.netValue();
			JPanel card = card();
			JPanel heading = heading(visit.getLocation().getDisplayName(),
				visit.getLocation().isPoh() ? ColorScheme.BRAND_ORANGE : Color.WHITE,
				signed(net) + " gp", net == 0 ? ColorScheme.LIGHT_GRAY_COLOR : net > 0 ? GAIN : LOSS);
			// On its own line, so a long location name can't push it out of view
			heading.add(small(Snapshot.ago(visit.getTakenAt()), ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.SOUTH);
			card.add(heading, BorderLayout.NORTH);
			card.add(rows(rows, true), BorderLayout.CENTER);
			content.add(card);
		}
		if (!any)
		{
			content.add(note(visits.isEmpty()
				? "No visits recorded yet. Take out or put in something at the bank or a POH storage, and it shows here."
				: "No matches."));
		}
	}

	private JPanel rows(List<StuffRow> rows, boolean changes)
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		JList<StuffRow> list = new JList<>(rows.subList(0, Math.min(rows.size(), MAX_ROWS)).toArray(new StuffRow[0]));
		list.setFixedCellHeight(ROW_HEIGHT);
		// Rows take the panel's width; long names are cut short ("...") instead of widening the list
		list.setFixedCellWidth(1);
		list.setCellRenderer(new RowRenderer(changes));
		list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.add(list, BorderLayout.CENTER);
		if (rows.size() > MAX_ROWS)
		{
			panel.add(small((rows.size() - MAX_ROWS) + " more: search to narrow", ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.SOUTH);
		}
		return panel;
	}

	private static JPanel card()
	{
		JPanel card = new JPanel(new BorderLayout(0, 4));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 6, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(6, 8, 6, 8)));
		card.setAlignmentX(LEFT_ALIGNMENT);
		return card;
	}

	private static JPanel heading(String left, Color leftColor, String right, Color rightColor)
	{
		JPanel heading = new JPanel(new BorderLayout(6, 0));
		heading.setOpaque(false);
		JLabel name = new JLabel(left);
		name.setForeground(leftColor);
		name.setToolTipText(left);
		shrinkable(name);
		heading.add(name, BorderLayout.CENTER);
		JLabel summary = new JLabel(right);
		summary.setFont(FontManager.getRunescapeSmallFont());
		summary.setForeground(rightColor);
		heading.add(summary, BorderLayout.EAST);
		return heading;
	}

	private Color freshness(long at)
	{
		long hours = (System.currentTimeMillis() - at) / 3_600_000;
		return hours >= staleHours ? STALE : hours >= warnHours ? WARN : ColorScheme.LIGHT_GRAY_COLOR;
	}

	private static JLabel note(String text)
	{
		JLabel label = small("<html>" + text + "</html>", ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(4, 0, 4, 0));
		return label;
	}

	private static JLabel small(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}

	/**
	 * Lets a label give up width (its text is cut short with "...") rather than push the layout wider.
	 */
	private static void shrinkable(JLabel label)
	{
		label.setPreferredSize(new Dimension(0, label.getPreferredSize().height));
		label.setMinimumSize(new Dimension(0, 0));
	}

	private static <T extends Component> JPanel leftAligned(T component)
	{
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setBorder(new EmptyBorder(0, 0, 6, 0));
		wrap.add(component, BorderLayout.CENTER);
		wrap.setAlignmentX(LEFT_ALIGNMENT);
		return wrap;
	}

	private static String gp(long value)
	{
		return QuantityFormatter.quantityToStackSize(value) + " gp";
	}

	private static String signed(long value)
	{
		return (value > 0 ? "+" : value < 0 ? "-" : "") + QuantityFormatter.quantityToStackSize(Math.abs(value));
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
				content.repaint();
			});
			return icon;
		});
	}

	/**
	 * Loot tracker style row: icon, name, and on the right the quantity over its value.
	 */
	private class RowRenderer extends JPanel implements ListCellRenderer<StuffRow>
	{
		private final boolean changes;
		private final JLabel icon = new JLabel();
		private final JLabel name = new JLabel();
		private final JLabel quantity = new JLabel();
		private final JLabel value = new JLabel();

		RowRenderer(boolean changes)
		{
			super(new BorderLayout(6, 0));
			this.changes = changes;
			setBorder(new EmptyBorder(2, 4, 2, 4));
			icon.setPreferredSize(new Dimension(36, 32));
			add(icon, BorderLayout.WEST);
			name.setForeground(Color.WHITE);
			shrinkable(name);
			add(name, BorderLayout.CENTER);
			JPanel right = new JPanel(new GridLayout(2, 1));
			right.setOpaque(false);
			quantity.setHorizontalAlignment(SwingConstants.RIGHT);
			value.setHorizontalAlignment(SwingConstants.RIGHT);
			value.setFont(FontManager.getRunescapeSmallFont());
			value.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			right.add(quantity);
			right.add(value);
			add(right, BorderLayout.EAST);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends StuffRow> list, StuffRow row, int index,
			boolean selected, boolean focus)
		{
			icon.setIcon(icon(row.getItemId()));
			name.setText(row.getName());
			if (changes)
			{
				quantity.setText(signed(row.getQuantity()));
				quantity.setForeground(row.getQuantity() > 0 ? GAIN : LOSS);
				value.setText(row.getUnitPrice() > 0 ? signed(row.totalValue()) + " gp" : "");
			}
			else
			{
				quantity.setText(QuantityFormatter.quantityToStackSize(row.getQuantity()));
				quantity.setForeground(Color.WHITE);
				value.setText(row.getUnitPrice() > 0 ? gp(row.totalValue()) : "");
			}
			String share = changes ? null : StuffGroups.share(row.totalValue(), grandTotal);
			setBackground(selected ? ColorScheme.DARK_GRAY_HOVER_COLOR
				: index % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
			setToolTipText(share == null ? row.getName() : row.getName() + " · " + share + " of everything you own");
			return this;
		}
	}

	/**
	 * Scroll content that's always exactly as wide as the sidebar, so nothing is ever cut off on the right.
	 */
	private static class WidthTrackingPanel extends JPanel implements Scrollable
	{
		WidthTrackingPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
		{
			return ROW_HEIGHT;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
		{
			return visible.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}

package com.ultimatebank.feature.storagesavings;

import com.ultimatebank.core.snapshots.Snapshot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Ranks storage locations by how many bank slots they would free. Click a location to list its items.
 * Must only be touched on the Swing thread.
 */
class StorageSavingsPanel extends PluginPanel
{
	private static final Color UPGRADE = new Color(120, 200, 120);

	private final JLabel summary = new JLabel();
	private final JPanel ranking = new JPanel();
	/** Locations the user has expanded, kept across updates */
	private final Set<String> expanded = new HashSet<>();

	private StorageSavingsState.Results results = StorageSavingsState.Results.EMPTY;

	/**
	 * @param openSettings opens the plugin settings, where storage locations and POH tiers are chosen
	 */
	StorageSavingsPanel(Runnable openSettings)
	{
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel header = new JPanel(new BorderLayout(0, 4));
		JLabel title = new JLabel("Ultimate Bank Savings");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(title, BorderLayout.NORTH);
		summary.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		header.add(summary, BorderLayout.CENTER);
		JButton settings = new JButton("Edit storage locations & tiers");
		settings.setToolTipText("Opens the settings: expand \"Ultimate Bank Savings: Locations\" or \"Ultimate Bank Savings: Tiers\"");
		settings.setFocusPainted(false);
		settings.addActionListener(e -> openSettings.run());
		header.add(settings, BorderLayout.SOUTH);
		add(header, BorderLayout.NORTH);

		ranking.setLayout(new BoxLayout(ranking, BoxLayout.Y_AXIS));
		add(ranking, BorderLayout.CENTER);

		rebuild();
	}

	void update(StorageSavingsState.Results results)
	{
		this.results = results;
		rebuild();
	}

	private void rebuild()
	{
		ranking.removeAll();

		if (!results.isBankSeen())
		{
			summary.setText("<html>Open your bank to scan it.</html>");
		}
		else
		{
			int count = results.getFlagged().size();
			String text = count == 0
				? "Nothing in your bank could be stored elsewhere."
				: count + (count == 1 ? " item" : " items") + " could be stored elsewhere. Click a location to see its items.";
			if (results.getAlreadyStored() > 0)
			{
				text += "<br>" + results.getAlreadyStored() + " already have copies stored in your POH.";
			}
			if (results.getBankSnapshotAt() > 0)
			{
				text += "<br><i>From your bank as last seen " + Snapshot.ago(results.getBankSnapshotAt()) + ".</i>";
			}
			summary.setText("<html>" + text + "</html>");
			for (StorageSavingsState.LocationSavings location : results.getRanking())
			{
				ranking.add(row(location));
			}
		}

		ranking.revalidate();
		ranking.repaint();
	}

	/**
	 * @return e.g. "Oak · +3 with Teak", "not built · +5 with Oak", "tier unknown: visit your costume room", or null
	 */
	private static String tierText(StorageSavingsState.LocationSavings location)
	{
		String tier = location.getTier();
		if (tier == null)
		{
			return null;
		}
		if ("tier unknown".equals(tier))
		{
			return "tier unknown: visit your costume room";
		}
		if (location.getNextTierExtra() > 0)
		{
			return tier + " · +" + location.getNextTierExtra() + " with " + location.getNextTier();
		}
		return tier;
	}

	private JPanel row(StorageSavingsState.LocationSavings location)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 4, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(6, 8, 6, 8)));

		int slots = location.getItemNames().size();
		JLabel name = new JLabel(location.getName());
		name.setForeground(location.isPoh() ? ColorScheme.BRAND_ORANGE : Color.WHITE);
		JLabel count = new JLabel(slots + (slots == 1 ? " slot" : " slots"));
		count.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		JPanel heading = new JPanel(new BorderLayout());
		heading.setOpaque(false);
		heading.add(name, BorderLayout.WEST);
		heading.add(count, BorderLayout.EAST);

		String tierText = tierText(location);
		if (tierText != null)
		{
			JLabel tier = new JLabel(tierText);
			tier.setFont(FontManager.getRunescapeSmallFont());
			tier.setForeground(location.getNextTierExtra() > 0 ? UPGRADE : ColorScheme.LIGHT_GRAY_COLOR);
			heading.add(tier, BorderLayout.SOUTH);
		}
		row.add(heading, BorderLayout.NORTH);

		boolean open = expanded.contains(location.getName());
		if (open)
		{
			JPanel items = new JPanel();
			items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
			items.setOpaque(false);
			items.setBorder(new EmptyBorder(4, 6, 0, 0));
			for (String itemName : location.getItemNames())
			{
				JLabel item = new JLabel(itemName);
				item.setFont(FontManager.getRunescapeSmallFont());
				item.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				items.add(item);
			}
			row.add(items, BorderLayout.CENTER);
		}

		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (!expanded.remove(location.getName()))
				{
					expanded.add(location.getName());
				}
				rebuild();
			}
		});
		row.setToolTipText(location.isPoh() ? "Player-owned house storage" : null);
		row.setAlignmentX(LEFT_ALIGNMENT);
		return row;
	}
}

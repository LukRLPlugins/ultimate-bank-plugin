package com.ultimatebank.feature.layouttools;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.UltimateBankPlugin;
import com.ultimatebank.core.BankFeature;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.KeyCode;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;

/**
 * Extra tools on top of core bank tag layouts: a zigzag style for core's "Auto layout" menu, and
 * shift-right-click "Insert empty row" / "Remove row" on empty space in a laid-out tab.
 * Ported from Bank Tag Layouts by geheur; see docs/features/layout-tools.md.
 */
@Slf4j
@Singleton
public class LayoutToolsFeature implements BankFeature
{
	/** Distinct from Bank Tag Layouts' own "Zigzag", since core rejects duplicate auto layout names */
	static final String ZIGZAG_NAME = "Zigzag (Ultimate Bank)";

	private static final int[] RUNE_POUCH_SLOTS = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4
	};
	private static final int ROW_HEIGHT = BankTagsPlugin.BANK_ITEM_HEIGHT + BankTagsPlugin.BANK_ITEM_Y_PADDING;

	@Inject
	private Client client;

	@Inject
	private ItemManager itemManager;

	@Inject
	private UltimateBankConfig config;

	@Inject
	private LayoutManager layoutManager;

	@Inject
	private BankTagsService bankTagsService;

	@Inject
	private UltimateBankPlugin plugin;

	private boolean zigzagRegistered;

	@Override
	public boolean isEnabled(UltimateBankConfig config)
	{
		return config.layoutToolsEnabled();
	}

	@Override
	public void startUp()
	{
		syncZigzag();
	}

	@Override
	public void shutDown()
	{
		if (zigzagRegistered)
		{
			layoutManager.unregisterAutoLayout(ZIGZAG_NAME);
			zigzagRegistered = false;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (UltimateBankConfig.GROUP.equals(event.getGroup()) && "layoutToolsZigzag".equals(event.getKey()))
		{
			syncZigzag();
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.layoutToolsRowMenus() || !client.isKeyPressed(KeyCode.KC_SHIFT))
		{
			return;
		}

		Layout layout = bankTagsService.getActiveLayout();
		Widget bankMenu = client.getWidget(InterfaceID.Bankmain.MENU_CONTAINER);
		if (layout == null || (bankMenu != null && !bankMenu.isHidden()))
		{
			return;
		}

		// Only on empty space: any entry with a widget means an item (or other component) was clicked
		for (MenuEntry entry : event.getMenuEntries())
		{
			if (entry.getWidget() != null)
			{
				return;
			}
		}

		int row = getMouseRow();
		int[] items = layout.getLayout();
		int rows = LayoutMath.rowCount(items);
		if (row < 0 || row >= rows)
		{
			return;
		}

		String tag = layout.getTag();
		client.getMenu().createMenuEntry(-1)
			.setOption("Insert empty row")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> saveAndReopen(tag, LayoutMath.insertRow(items, row)));

		if (row < rows - 1 && LayoutMath.isRowEmpty(items, row))
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("Remove row")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> saveAndReopen(tag, LayoutMath.removeRow(items, row)));
		}
	}

	private void syncZigzag()
	{
		if (config.layoutToolsZigzag() && !zigzagRegistered)
		{
			try
			{
				layoutManager.registerAutoLayout(plugin, ZIGZAG_NAME, this::zigzag);
				zigzagRegistered = true;
			}
			catch (IllegalArgumentException e)
			{
				log.warn("Couldn't register the zigzag auto layout", e);
			}
		}
		else if (!config.layoutToolsZigzag() && zigzagRegistered)
		{
			layoutManager.unregisterAutoLayout(ZIGZAG_NAME);
			zigzagRegistered = false;
		}
	}

	/**
	 * Core calls this on the client thread when the user picks the zigzag auto layout, then offers Keep/Undo.
	 */
	private Layout zigzag(Layout previous)
	{
		int[] inventory = itemIds(client.getItemContainer(InventoryID.INV));
		int[] runes = config.layoutToolsZigzagRunePouch() && hasRunePouch(inventory) ? runePouchRunes() : null;
		int[] layout = LayoutMath.zigzag(itemIds(client.getItemContainer(InventoryID.WORN)), inventory, runes,
			previous.getLayout(), config.layoutToolsZigzagDuplicateLimit());
		return new Layout(previous.getTag(), layout);
	}

	private void saveAndReopen(String tag, int[] items)
	{
		layoutManager.saveLayout(new Layout(tag, items));
		bankTagsService.openBankTag(tag, BankTagsService.OPTION_ALLOW_MODIFICATIONS);
	}

	/**
	 * @return item IDs by slot, canonicalized (so noted or worn variants match banked items), -1 for empty slots
	 */
	private int[] itemIds(ItemContainer container)
	{
		if (container == null)
		{
			return new int[0];
		}
		Item[] items = container.getItems();
		int[] ids = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			int id = items[i].getId();
			ids[i] = id > 0 ? itemManager.canonicalize(id) : -1;
		}
		return ids;
	}

	private static boolean hasRunePouch(int[] inventory)
	{
		Collection<Integer> pouches = ItemVariationMapping.getVariations(ItemID.BH_RUNE_POUCH);
		Collection<Integer> divinePouches = ItemVariationMapping.getVariations(ItemID.DIVINE_RUNE_POUCH);
		for (int id : inventory)
		{
			if (pouches.contains(id) || divinePouches.contains(id))
			{
				return true;
			}
		}
		return false;
	}

	private int[] runePouchRunes()
	{
		EnumComposition runeEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		return Arrays.stream(RUNE_POUCH_SLOTS)
			.map(client::getVarbitValue)
			.filter(rune -> rune > 0)
			.map(runeEnum::getIntValue)
			.toArray();
	}

	/**
	 * @return the bank row under the mouse in the laid-out tab, or -1 if the mouse isn't over the bank items
	 */
	private int getMouseRow()
	{
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (container == null || container.isHidden())
		{
			return -1;
		}

		Point mouse = client.getMouseCanvasPosition();
		Rectangle bounds = container.getBounds();
		if (!bounds.contains(mouse.getX(), mouse.getY()))
		{
			return -1;
		}

		// Items are drawn with half the row padding above them
		int y = mouse.getY() - container.getCanvasLocation().getY() + container.getScrollY()
			- BankTagsPlugin.BANK_ITEM_START_Y + BankTagsPlugin.BANK_ITEM_Y_PADDING / 2;
		return y / ROW_HEIGHT;
	}
}

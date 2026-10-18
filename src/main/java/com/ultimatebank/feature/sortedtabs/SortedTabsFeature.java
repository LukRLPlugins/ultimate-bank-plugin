package com.ultimatebank.feature.sortedtabs;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.core.BankFeature;
import com.ultimatebank.core.ExtraTabs;
import com.ultimatebank.feature.sortedtabs.data.DefaultTabs;
import com.ultimatebank.feature.sortedtabs.data.ItemMeta;
import com.ultimatebank.feature.sortedtabs.data.SortData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

/**
 * Auto-sorted skill tabs ("melee (ub)", ...) with titled sections, built from Bank Skill Sorting's data plus the
 * user's {@link TabEdits}. Tabs are virtual bank tags (no item tags are written); layouts are rebuilt lazily when a
 * tab is viewed after the bank changed. See docs/features/sorted-tabs.md.
 */
@Slf4j
@Singleton
public class SortedTabsFeature implements BankFeature
{
	static final String TAG_SUFFIX = " (ub)";
	private static final String KEY_PREFIX = "sortedTabs";
	private static final String EDITS_KEY_PREFIX = "sortedTabsEdits.";
	/** Tabs we added to the Bank Tags tab list, so we only ever remove our own */
	private static final String CREATED_KEY = "sortedTabsCreated";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private UltimateBankConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Gson gson;

	@Inject
	private TagManager tagManager;

	@Inject
	private LayoutManager layoutManager;

	@Inject
	private BankTagsService bankTagsService;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SortedTabsOverlay overlay;

	@Inject
	private SortedTabsState state;

	@Inject
	private SortedLayoutBuilder layoutBuilder;

	@Inject
	private ExtraTabs extraTabs;

	private final Runnable extraTabsListener = this::onExtraTabsChanged;

	/** Default tabs by standardized bank tag name */
	private final Map<String, DefaultTabs.Tab> tabsByTag = new LinkedHashMap<>();
	/** Internal tab id ("melee", "storable") of each registered tab */
	private final Map<String, String> tabIds = new HashMap<>();
	/** Tags currently registered with Bank Tags */
	private final Set<String> registered = new HashSet<>();
	/** Resolved sections of each registered tab */
	private final Map<String, List<TabResolver.ResolvedSection>> resolved = new HashMap<>();
	/** Every item in each registered tab, for fast membership tests */
	private final Map<String, Set<Integer>> members = new HashMap<>();
	/** Tabs whose layout is stale (bank or edits changed since it was built) */
	private final Set<String> dirty = new HashSet<>();

	private long bankFingerprint;
	private boolean bankOpen;
	private String lastActiveTag;
	private boolean rebuildPending;

	@Override
	public boolean isEnabled(UltimateBankConfig config)
	{
		return config.sortedTabsEnabled();
	}

	@Override
	public void startUp()
	{
		// Loaded here rather than at plugin start, so users without this feature don't pay for the 1 MB parse
		SortData.initItemMeta(gson);
		tabsByTag.clear();
		for (DefaultTabs.Tab tab : DefaultTabs.load(gson))
		{
			tabsByTag.put(Text.standardize(bankTag(tab.getId())), tab);
		}
		log.debug("Loaded {} sorted tabs, {} item metadata entries", tabsByTag.size(), SortData.itemMeta().size());

		syncTabs(true, true);
		extraTabs.addListener(extraTabsListener);
		extraTabs.setDisplayed(true);
		overlayManager.add(overlay);
		bankFingerprint = 0;
		lastActiveTag = null;
		bankOpen = client.getWidget(InterfaceID.Bankmain.ITEMS) != null;
	}

	@Override
	public void shutDown()
	{
		overlayManager.remove(overlay);
		for (String tag : new ArrayList<>(registered))
		{
			unregister(tag);
		}
		removeAllCreatedTabs();
		extraTabs.removeListener(extraTabsListener);
		extraTabs.setDisplayed(false);
		state.clear();
		resolved.clear();
		tabIds.clear();
		members.clear();
		dirty.clear();
		rebuildPending = false;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		String key = event.getKey();
		if (!UltimateBankConfig.GROUP.equals(event.getGroup()) || !key.startsWith(KEY_PREFIX)
			|| key.startsWith(EDITS_KEY_PREFIX) || key.equals(CREATED_KEY) || key.equals("sortedTabsEnabled"))
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			syncTabs(true, true);
			dirty.addAll(registered);
			rebuildPending = true;
		});
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.BANK)
		{
			return;
		}

		// The bank container also fires on every open with unchanged contents; rebuilding then causes a hitch
		long fingerprint = fingerprint(event.getItemContainer());
		if (fingerprint == bankFingerprint)
		{
			return;
		}
		bankFingerprint = fingerprint;
		dirty.addAll(registered);
		// Wait a tick so every other subscriber (e.g. Bank Tags' own tagging) has seen the change first
		rebuildPending = true;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!bankOpen)
		{
			return;
		}

		String active = activeManagedTag();
		boolean switched = active != null && !active.equals(lastActiveTag);
		lastActiveTag = active;
		if (active != null && dirty.contains(active) && (rebuildPending || switched))
		{
			rebuild(active);
			bankTagsService.openBankTag(bankTagsService.getActiveTag(), BankTagsService.OPTION_ALLOW_MODIFICATIONS);
		}
		rebuildPending = false;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = true;
			lastActiveTag = null;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankOpen = false;
			lastActiveTag = null;
		}
	}

	/**
	 * Another feature published, changed or withdrew a tab (e.g. Storage savings' storable items).
	 */
	private void onExtraTabsChanged()
	{
		syncTabs(false, true);
		for (ExtraTabs.Tab tab : extraTabs.getTabs())
		{
			dirty.add(Text.standardize(bankTag(tab.getId())));
		}
		rebuildPending = true;
	}

	/**
	 * Registers enabled tabs and unregisters disabled ones, keeping the Bank Tags tab list in step.
	 *
	 * @param defaults re-resolve the default skill tabs (after config or edits change)
	 * @param extras   refresh tabs published by other features
	 */
	private void syncTabs(boolean defaults, boolean extras)
	{
		List<String> tabList = getTabList();
		Set<String> created = getCreated();
		Set<String> seen = new HashSet<>();

		if (defaults)
		{
			for (DefaultTabs.Tab tab : tabsByTag.values())
			{
				String tag = Text.standardize(bankTag(tab.getId()));
				seen.add(tag);
				apply(tag, tab.getId(), tab.getIcon(), isTabEnabled(tab.getId()) ? resolve(tab) : null, tabList, created);
			}
		}

		if (extras)
		{
			for (ExtraTabs.Tab tab : extraTabs.getTabs())
			{
				String tag = Text.standardize(bankTag(tab.getId()));
				seen.add(tag);
				List<TabResolver.ResolvedSection> sections = new ArrayList<>();
				for (ExtraTabs.Section section : tab.getSections())
				{
					sections.add(new TabResolver.ResolvedSection(section.getTitle(), section.getTitle(), 1, null, section.getItems()));
				}
				apply(tag, tab.getId(), tab.getIcon(), isTabEnabled(tab.getId()) ? sections : null, tabList, created);
			}

			// Published tabs that were withdrawn
			for (String tag : new ArrayList<>(registered))
			{
				boolean isDefault = tabsByTag.containsKey(tag);
				if (!isDefault && !seen.contains(tag))
				{
					apply(tag, null, 0, null, tabList, created);
				}
			}
		}

		saveTabList(tabList);
		saveCreated(created);
	}

	/**
	 * @param sections the tab's sections, or null to remove the tab
	 */
	private void apply(String tag, String tabId, int icon, List<TabResolver.ResolvedSection> sections,
		List<String> tabList, Set<String> created)
	{
		if (sections == null)
		{
			unregister(tag);
			if (created.remove(tag))
			{
				removeTab(tabList, tag);
			}
			return;
		}

		resolved.put(tag, sections);
		tabIds.put(tag, tabId);
		Set<Integer> tabMembers = new HashSet<>();
		sections.forEach(section -> tabMembers.addAll(section.getItems()));
		members.put(tag, tabMembers);
		if (!registered.contains(tag))
		{
			register(tag);
		}
		if (!containsTab(tabList, tag))
		{
			tabList.add(bankTag(tabId));
			created.add(tag);
			configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_ICON_PREFIX + tag, icon);
		}
	}

	private void register(String tag)
	{
		tagManager.registerTag(tag, itemId -> isMember(tag, itemId));
		registered.add(tag);
		dirty.add(tag);
	}

	private void unregister(String tag)
	{
		if (registered.remove(tag))
		{
			tagManager.unregisterTag(tag);
		}
		resolved.remove(tag);
		tabIds.remove(tag);
		members.remove(tag);
		dirty.remove(tag);
		state.setHeaders(tag, Collections.emptyMap());
	}

	/**
	 * Bank Tags' membership test for our virtual tags. Called on the client thread with bank item IDs (which may be
	 * placeholders); variants count as members when their base item is.
	 */
	private boolean isMember(String tag, int itemId)
	{
		Set<Integer> tabMembers = members.get(tag);
		if (tabMembers == null)
		{
			return false;
		}
		int canonical = itemManager.canonicalize(itemId);
		return tabMembers.contains(canonical) || tabMembers.contains(ItemVariationMapping.map(canonical));
	}

	private void rebuild(String tag)
	{
		String tabId = tabIds.get(tag);
		List<TabResolver.ResolvedSection> sections = resolved.get(tag);
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (tabId == null || sections == null || bank == null)
		{
			return;
		}

		SortedLayoutBuilder.Built built = layoutBuilder.build(tag, tabId, sections, ownedByBase(bank),
			config.sortedTabsTitles(), config.sortedTabsLowerTier());
		layoutManager.saveLayout(built.getLayout());
		state.setHeaders(tag, built.getHeaders());
		dirty.remove(tag);
		log.debug("Rebuilt sorted tab '{}': {} titles", tag, built.getHeaders().size());
	}

	/**
	 * Variation base ID to every ID of that family in the bank, base first, so variants share their base's spot.
	 */
	private Map<Integer, List<Integer>> ownedByBase(ItemContainer bank)
	{
		Map<Integer, List<Integer>> owned = new HashMap<>();
		for (Item item : bank.getItems())
		{
			if (item.getId() <= 0)
			{
				continue;
			}
			int canonical = itemManager.canonicalize(item.getId());
			int base = ItemVariationMapping.map(canonical);
			List<Integer> family = owned.computeIfAbsent(base, k -> new ArrayList<>());
			if (family.contains(canonical))
			{
				continue;
			}
			if (canonical == base)
			{
				family.add(0, canonical);
			}
			else
			{
				family.add(canonical);
			}
		}
		return owned;
	}

	private List<TabResolver.ResolvedSection> resolve(DefaultTabs.Tab tab)
	{
		List<String> order = SortData.TAB_SECTIONS.getOrDefault(tab.getId(), Collections.emptyList());
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		return TabResolver.resolve(tab.getItems(), order, itemId ->
		{
			ItemMeta m = meta.get(itemId);
			return m != null && m.sections != null ? m.sections.get(tab.getId()) : null;
		}, loadEdits(tab));
	}

	private TabEdits loadEdits(DefaultTabs.Tab tab)
	{
		String json = configManager.getConfiguration(UltimateBankConfig.GROUP, EDITS_KEY_PREFIX + tab.getId());
		if (Strings.isNullOrEmpty(json))
		{
			return null;
		}
		try
		{
			return gson.fromJson(json, TabEdits.class);
		}
		catch (Exception e)
		{
			log.warn("Ignoring unreadable edits for sorted tab {}", tab.getId(), e);
			return null;
		}
	}

	private boolean isTabEnabled(String tabId)
	{
		Boolean enabled = configManager.getConfiguration(UltimateBankConfig.GROUP, toggleKey(tabId), Boolean.class);
		return enabled == null || enabled;
	}

	/**
	 * @return the standardized tag of the sorted tab being viewed, or null
	 */
	private String activeManagedTag()
	{
		String active = bankTagsService.getActiveTag();
		if (active == null)
		{
			return null;
		}
		String tag = Text.standardize(active);
		return registered.contains(tag) ? tag : null;
	}

	private void removeAllCreatedTabs()
	{
		List<String> tabList = getTabList();
		Set<String> created = getCreated();
		for (String tag : created)
		{
			removeTab(tabList, tag);
		}
		saveTabList(tabList);
		configManager.unsetConfiguration(UltimateBankConfig.GROUP, CREATED_KEY);
	}

	private void removeTab(List<String> tabList, String tag)
	{
		tabList.removeIf(t -> Text.standardize(t).equals(tag));
		configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_ICON_PREFIX + tag);
		layoutManager.removeLayout(tag);
		if (tag.equals(Text.standardize(Strings.nullToEmpty(bankTagsService.getActiveTag()))))
		{
			bankTagsService.closeBankTag();
		}
	}

	private static boolean containsTab(List<String> tabList, String tag)
	{
		return tabList.stream().anyMatch(t -> Text.standardize(t).equals(tag));
	}

	private List<String> getTabList()
	{
		String csv = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_TABS_CONFIG);
		return new ArrayList<>(Text.fromCSV(Strings.nullToEmpty(csv)));
	}

	private void saveTabList(List<String> tabList)
	{
		configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_TABS_CONFIG, Text.toCSV(tabList));
	}

	private Set<String> getCreated()
	{
		String csv = configManager.getConfiguration(UltimateBankConfig.GROUP, CREATED_KEY);
		return new LinkedHashSet<>(Text.fromCSV(Strings.nullToEmpty(csv)));
	}

	private void saveCreated(Set<String> created)
	{
		configManager.setConfiguration(UltimateBankConfig.GROUP, CREATED_KEY, Text.toCSV(created));
	}

	/**
	 * @return the Bank Tags name for a tab id, e.g. "woodcutting + firemaking (ub)"
	 */
	static String bankTag(String tabId)
	{
		return tabId.replace("_", " + ") + TAG_SUFFIX;
	}

	/**
	 * @return e.g. "sortedTabsWoodcuttingFiremaking", matching the toggles in {@link UltimateBankConfig}
	 */
	private static String toggleKey(String tabId)
	{
		StringBuilder key = new StringBuilder(KEY_PREFIX);
		for (String word : tabId.split("_"))
		{
			key.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return key.toString();
	}

	private static long fingerprint(ItemContainer bank)
	{
		long h = 1125899906842597L;
		if (bank != null)
		{
			for (Item item : bank.getItems())
			{
				h = 31 * h + item.getId();
				h = 31 * h + item.getQuantity();
			}
		}
		return h;
	}
}

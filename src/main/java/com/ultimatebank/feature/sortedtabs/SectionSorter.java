/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2026, Frailrain
 * Copyright (c) 2026, Luk
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ultimatebank.feature.sortedtabs;

import com.ultimatebank.UltimateBankConfig;
import com.ultimatebank.feature.sortedtabs.data.ItemMeta;
import com.ultimatebank.feature.sortedtabs.data.SortData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;

/**
 * Sorting rules for Bank Skill Sorting's default sections: hand-authored orders (herbs, potions, food, runes, gems,
 * weapon classes, ...) and the combat tabs' loadout / lower-tier split. Ported from Bank Skill Sorting's
 * SkillBankLayoutBuilder (Frailrain); the layout building itself lives in {@link SortedLayoutBuilder}.
 */
@Slf4j
@Singleton
public class SectionSorter
{
	private final ItemManager itemManager;
	private final UltimateBankConfig config;

	private final Map<String, Integer> herbOrderIndex;
	private final Map<String, Integer> potionOrderIndex;
	private final Map<String, Integer> foodOrderIndex;
	private final Map<String, Integer> runeOrderIndex;
	private final Map<String, Integer> comboRuneOrderIndex;
	private final Map<String, Integer> gemOrderIndex;

	@Inject
	SectionSorter(ItemManager itemManager, UltimateBankConfig config)
	{
		this.itemManager = itemManager;
		this.config = config;
		this.herbOrderIndex = lowercaseIndex(SortData.HERB_ORDER);
		this.potionOrderIndex = lowercaseIndex(SortData.POTION_ORDER);
		this.foodOrderIndex = lowercaseIndex(SortData.FOOD_ORDER);
		this.runeOrderIndex = lowercaseIndex(SortData.RUNE_ORDER);
		this.comboRuneOrderIndex = lowercaseIndex(SortData.COMBO_RUNE_ORDER);
		this.gemOrderIndex = lowercaseIndex(SortData.GEM_ORDER);
	}

	private static Map<String, Integer> lowercaseIndex(List<String> order)
	{
		Map<String, Integer> m = new HashMap<>();
		for (int i = 0; i < order.size(); i++)
		{
			m.put(order.get(i).toLowerCase(Locale.ROOT), i);
		}
		return m;
	}

	/**
	 * Sorts a default section's items in place by that section's hand-authored rules.
	 *
	 * @param section the default section name (sort domain), as in {@link SortData#TAB_SECTIONS}
	 * @param tabId   the default tab id, e.g. "melee"
	 */
	public void sort(List<Integer> items, String section, String tabId)
	{
		sortSection(items, section, tabId);
	}

	/**
	 * Splits a combat tab's default section into loadout items (zone 1) and lower-tier items (zone 2).
	 * Only meaningful for {@link SortData#TWO_ZONE_TABS}.
	 */
	public void partition(List<Integer> items, String section, String tabId, List<Integer> zone1, List<Integer> zone2)
	{
		partitionByRequirement(items, section, tabId, zone1, zone2);
	}

	// ── Sorting helpers ────────────────────────────────────────────────────

	/** Sorts items in place. Returns the name of the comparator used so the
	 *  trace can show which sort domain fired for each section. */
	private String sortSection(List<Integer> items, String section, String tagName)
	{
		// Brief #87: range tab's nine weapon rows use a fixed launcher→ammo
		// ordering. Section name carries the row identity; per-section
		// sub-rank + tier ASC keep the within-row order predictable.
		if ("range".equals(tagName) && RANGE_WEAPON_SECTIONS.contains(section))
		{
			items.sort((a, b) -> compareRangeWeapon(a, b, section));
			return "RANGE_WEAPON_FIXED";
		}
		// Brief #75 (revised): fletching uses a per-section comparator with
		// tier ASC + sub-rank groupings (shafts/tips/unstrung etc.).
		if ("fletching".equals(tagName))
		{
			items.sort((a, b) -> compareFletching(a, b, section));
			return "FLETCHING_PER_SECTION";
		}
		// Brief #75: cosmetics sorts by set rank → set name → slot rank →
		// item name so items in the same set cluster contiguously. The
		// layout builder then injects a row break between sets.
		if ("cosmetics".equals(tagName))
		{
			items.sort((a, b) -> compareCosmetics(a, b, section));
			return "COSMETICS_SET+SLOT";
		}
		if ("herblore".equals(tagName) && "Herbs".equals(section))
		{
			items.sort(this::compareHerbs);
			return "HERB_ORDER";
		}
		// Brief #66: simple-mode Armor section sorts head → body → legs →
		// hands → feet, then by tier within slot.
		if ("Armor".equals(section))
		{
			items.sort(this::compareArmorSlotThenTier);
			return "ARMOR_SLOT+tier_desc";
		}
		if ("Finished potions".equals(section)
			|| "Barbarian mixes".equals(section)
			|| "Divine, extended & upgraded variants".equals(section)
			|| "Combat potions".equals(section)
			|| "Potions".equals(section)
			|| "Prayer & restores".equals(section)
			|| "Prayer-restoring consumables".equals(section)
			|| "Run-energy consumables".equals(section))
		{
			items.sort(this::comparePotions);
			return "POTION_ORDER";
		}
		// Brief #90: the cooking tab's six family rows (fish / meat ×
		// raw / cooked / burnt) sort by Cooking level, lowest first.
		if ("cooking".equals(tagName)
			&& ("Raw fish".equals(section) || "Cooked fish".equals(section)
				|| "Burnt fish".equals(section) || "Raw meat".equals(section)
				|| "Cooked meat".equals(section) || "Burnt meat".equals(section)))
		{
			items.sort(this::compareCookingLevel);
			return "COOKING_LEVEL_ORDER";
		}
		// Brief #66: composite cooking sections keep the canonical heal
		// order; combat-tab Food sections likewise.
		if (("cooking".equals(tagName) &&
				("Combo food".equals(section)
					|| "Baked & cooked goods".equals(section)))
			|| "Food".equals(section))
		{
			items.sort(this::compareFood);
			return "FOOD_ORDER";
		}
		if ("Runes".equals(section) || "Core runes".equals(section))
		{
			items.sort(this::compareRunes);
			return "RUNE_ORDER";
		}
		if ("Combination runes".equals(section))
		{
			items.sort(this::compareComboRunes);
			return "COMBO_RUNE_ORDER";
		}
		if ("Gems".equals(section))
		{
			items.sort(this::compareGems);
			return "GEM_ORDER";
		}
		if ("Weapons".equals(section))
		{
			items.sort(this::compareWeapons);
			return "WEAPON_CLASS_ORDER+tier_desc";
		}
		items.sort(this::compareByTierDescThenName);
		return "tier_desc+name";
	}

	/** Compute the top-N tier set per group within a section and split items. */
	private void partitionTopTiers(
		List<Integer> sectionItems, String section, String tagName,
		List<Integer> zone1, List<Integer> zone2)
	{
		int n = topNForSection(section, tagName);
		Map<Integer, ItemMeta> meta = SortData.itemMeta();

		// Group by weapon class for Weapons section; one logical group for
		// everything else (we still apply top-N to that one group).
		Map<String, List<Integer>> byGroup = new LinkedHashMap<>();
		if ("Weapons".equals(section))
		{
			for (Integer iid : sectionItems)
			{
				ItemMeta m = meta.get(iid);
				String g = (m != null && m.weaponClass != null) ? m.weaponClass : "_other";
				byGroup.computeIfAbsent(g, k -> new ArrayList<>()).add(iid);
			}
		}
		else
		{
			byGroup.put(section, sectionItems);
		}

		for (Map.Entry<String, List<Integer>> e : byGroup.entrySet())
		{
			List<Integer> group = e.getValue();
			Set<Integer> topTiers = computeTopTiers(group, n);
			for (Integer iid : group)
			{
				ItemMeta m = meta.get(iid);
				int t = m != null ? m.tier : 0;
				if (t > 0 && topTiers.contains(t))
				{
					zone1.add(iid);
				}
				else
				{
					zone2.add(iid);
				}
			}
		}
	}

	private Set<Integer> computeTopTiers(List<Integer> group, int n)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		Set<Integer> allTiers = new TreeSet<>(Comparator.reverseOrder());
		for (Integer iid : group)
		{
			ItemMeta m = meta.get(iid);
			if (m != null && m.tier > 0)
			{
				allTiers.add(m.tier);
			}
		}
		Set<Integer> top = new HashSet<>();
		int taken = 0;
		for (Integer t : allTiers)
		{
			if (taken++ >= n)
			{
				break;
			}
			top.add(t);
		}
		return top;
	}

	private int topNForSection(String section, String tagName)
	{
		// Combat tabs surface more endgame than skilling tabs.
		if ("melee".equals(tagName) || "range".equals(tagName) || "mage".equals(tagName))
		{
			return 3;
		}
		return 2;
	}

	// ── Brief #73: top-N unique levels dynamic zone partition ──────────────

	/** Sections in any combat tab that always route to Zone 1, regardless
	 *  of requirement. Endgame items here mostly carry no level requirement
	 *  and the sections are small enough to surface everything. Brief #78
	 *  added Food — cooked food never has equipment requirements so the
	 *  level-based partitioner would dump it into Zone 2 by default. */
	private static final Set<String> ALWAYS_ZONE1_SECTIONS = Set.of(
		// Brief #90: Potions rows directly after Food — consumables carry
		// no equipment requirement, so the level partitioner would
		// otherwise dump them into Zone 2.
		"Capes", "Neck", "Rings", "Food", "Potions",
		// Brief #87: range tab's nine fixed launcher→ammo rows skip the
		// dynamic top-N partition. Self-organising row structure
		// replaces it.
		"Bows", "Arrows", "Crossbows", "Bolts",
		"Ballistae & javelins", "Blowpipe & darts",
		"Knives", "Morrigan's javelins", "Other throwables"
	);

	/** Force-Zone-1 for items with no requirement data but high tier. Catches
	 *  Fire cape / Barrows gloves / Void / Ava's family — quest- or
	 *  activity-gated endgame items that have no level requirement. */
	private static final int NO_REQ_TIER_FORCE_FLOOR = 80;

	private static boolean isCombatTab(String tag)
	{
		return "melee".equals(tag) || "range".equals(tag) || "mage".equals(tag);
	}

	/**
	 * Brief #73: dynamic zone split that scales with player progression.
	 * For each section (independently), collect the unique requirement
	 * levels owned, sort descending, take the top N. Items at those levels
	 * are Zone 1; everything else is Zone 2. N comes from
	 * {@link UltimateBankConfig#sortedTabsLowerTierCount()} (default 3, range 1..6).
	 * <p>
	 * Capes/Neck/Rings always Zone 1. Mage Runes section: no zoning (all
	 * runes useful). Training & utility: always Zone 2. High-tier
	 * (tier ≥ 80) items with no requirement always Zone 1 (fire cape, etc.).
	 */
	private void partitionByRequirement(
		List<Integer> sectionItems, String section, String tagName,
		List<Integer> zone1, List<Integer> zone2)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();

		// Capes / Neck / Rings → every item to Zone 1.
		if (ALWAYS_ZONE1_SECTIONS.contains(section))
		{
			zone1.addAll(sectionItems);
			return;
		}
		// Training & utility (or anything else with that name) → always chaff.
		if (section != null
			&& (section.contains("Training") || section.contains("utility")))
		{
			zone2.addAll(sectionItems);
			return;
		}
		// Mage Runes section → no zoning (all runes are loadout-relevant).
		if ("Runes".equals(section) && "mage".equals(tagName))
		{
			zone1.addAll(sectionItems);
			return;
		}

		int n = Math.max(1, config.sortedTabsLowerTierCount());

		// Collect unique requirement levels owned in this section, in descending
		// order. TreeSet with reverse-comparator does both the dedup and sort.
		java.util.TreeSet<Integer> uniqueLevels = new java.util.TreeSet<>(
			java.util.Comparator.reverseOrder());
		for (Integer iid : sectionItems)
		{
			ItemMeta m = meta.get(iid);
			if (m == null)
			{
				continue;
			}
			uniqueLevels.add(effectiveReq(m, section, tagName));
		}

		// Top N unique levels — anything else lives in Zone 2.
		Set<Integer> topLevels = new HashSet<>();
		int taken = 0;
		for (Integer lv : uniqueLevels)
		{
			if (taken++ >= n) break;
			topLevels.add(lv);
		}

		for (Integer iid : sectionItems)
		{
			ItemMeta m = meta.get(iid);
			if (m == null)
			{
				zone2.add(iid);
				continue;
			}
			int req = effectiveReq(m, section, tagName);
			// High-tier quest-gated / activity-gated items always surface.
			if (req == 0 && m.tier >= NO_REQ_TIER_FORCE_FLOOR)
			{
				zone1.add(iid);
				continue;
			}
			if (topLevels.contains(req))
			{
				zone1.add(iid);
			}
			else
			{
				zone2.add(iid);
			}
		}
	}

	/**
	 * Brief #73: effective requirement level for the zone partition.
	 *  - Melee Weapons: max(attack, strength) — covers warhammers (Strength-
	 *    gated) and normal weapons (Attack-gated) on the same scale.
	 *  - Range armour: max(defence, ranged) — mid-tier range gear gates on
	 *    Ranged, high-tier on Defence + Ranged.
	 *  - Mage armour: max(magic, defence) — mage robes gate on Magic, tankier
	 *    mage armour adds Defence.
	 *  - Weapons / Ammunition: the tab's offensive skill.
	 *  - Other (head/body/legs/hands/feet/shield): defence.
	 */
	private int effectiveReq(ItemMeta m, String section, String tagName)
	{
		if ("Weapons".equals(section))
		{
			if ("melee".equals(tagName))
			{
				return Math.max(m.requirement("attack"), m.requirement("strength"));
			}
			if ("range".equals(tagName)) return m.requirement("ranged");
			if ("mage".equals(tagName))  return m.requirement("magic");
			return m.requirement("attack");
		}
		if ("Ammunition".equals(section))
		{
			return "range".equals(tagName) ? m.requirement("ranged")
				: m.requirement("attack");
		}
		if (isArmourSection(section))
		{
			if ("range".equals(tagName))
			{
				return Math.max(m.requirement("defence"), m.requirement("ranged"));
			}
			if ("mage".equals(tagName))
			{
				return Math.max(m.requirement("magic"), m.requirement("defence"));
			}
		}
		return m.requirement("defence");
	}

	private static boolean isArmourSection(String section)
	{
		return "Head".equals(section) || "Body".equals(section)
			|| "Legs".equals(section) || "Hands".equals(section)
			|| "Feet".equals(section)
			|| "Shields & defenders".equals(section)
			|| "Shields & off-hands".equals(section)
			|| "Off-hands, books & tomes".equals(section);
	}

	/** Kept for the older Brief #69 callsites — returns the *primary* skill
	 *  for a combat (section, tab) pair (does NOT take the dual-skill max).
	 *  Currently unused at runtime; left in for trace-log inspection. */
	@SuppressWarnings("unused")
	private static String combatRequirementSkill(String section, String tagName)
	{
		if ("Weapons".equals(section) || "Ammunition".equals(section))
		{
			if ("range".equals(tagName)) return "ranged";
			if ("mage".equals(tagName)) return "magic";
			return "attack";
		}
		// Head, Body, Legs, Hands, Feet, Shields & defenders, Shields & off-hands,
		// Off-hands & books & tomes — all gate on defence.
		return "defence";
	}

	// ── Comparators ────────────────────────────────────────────────────────

	private int compareHerbs(int a, int b)
	{
		String na = nameOf(a).toLowerCase(Locale.ROOT);
		String nb = nameOf(b).toLowerCase(Locale.ROOT);
		int ia = herbBaseRank(na);
		int ib = herbBaseRank(nb);
		if (ia != ib)
		{
			return Integer.compare(ia, ib);
		}
		// Same herb → Grimy first, then Clean.
		boolean grimyA = na.startsWith("grimy ");
		boolean grimyB = nb.startsWith("grimy ");
		if (grimyA != grimyB)
		{
			return grimyA ? -1 : 1;
		}
		return na.compareTo(nb);
	}

	private int herbBaseRank(String nameLower)
	{
		for (Map.Entry<String, Integer> e : herbOrderIndex.entrySet())
		{
			// Match "guam", "marrentill", etc. anywhere in the name (handles
			// "Grimy guam", "Clean guam", "Guam potion (unf)", etc.).
			if (nameLower.contains(e.getKey()))
			{
				return e.getValue();
			}
		}
		return Integer.MAX_VALUE;
	}

	private int comparePotions(int a, int b)
	{
		String na = nameOf(a).toLowerCase(Locale.ROOT);
		String nb = nameOf(b).toLowerCase(Locale.ROOT);
		int ia = potionFamilyRank(na);
		int ib = potionFamilyRank(nb);
		if (ia != ib)
		{
			return Integer.compare(ia, ib);
		}
		// Same family → dose 4 → 1.
		int da = potionDose(na);
		int db = potionDose(nb);
		if (da != db)
		{
			return Integer.compare(db, da);
		}
		return na.compareTo(nb);
	}

	private int potionFamilyRank(String nameLower)
	{
		int best = Integer.MAX_VALUE;
		for (Map.Entry<String, Integer> e : potionOrderIndex.entrySet())
		{
			if (nameLower.contains(e.getKey()) && e.getValue() < best)
			{
				best = e.getValue();
			}
		}
		return best;
	}

	private int potionDose(String nameLower)
	{
		int len = nameLower.length();
		if (len >= 3 && nameLower.charAt(len - 1) == ')'
			&& nameLower.charAt(len - 3) == '(')
		{
			char c = nameLower.charAt(len - 2);
			if (c >= '1' && c <= '4')
			{
				return c - '0';
			}
		}
		return 0;
	}

	/** Brief #90: cooking-level comparator for the cooking tab's family
	 *  rows. Longest COOKING_LEVEL_ORDER token contained in the name wins,
	 *  so "cave eel" beats "eel" and "bird meat" beats "meat". Unmatched
	 *  names sink to the end of the row, alphabetically. */
	private int compareCookingLevel(int a, int b)
	{
		String na = nameOf(a).toLowerCase(Locale.ROOT);
		String nb = nameOf(b).toLowerCase(Locale.ROOT);
		int ia = cookingLevelRank(na);
		int ib = cookingLevelRank(nb);
		if (ia != ib)
		{
			return Integer.compare(ia, ib);
		}
		return na.compareTo(nb);
	}

	private static int cookingLevelRank(String nameLower)
	{
		int best = Integer.MAX_VALUE;
		int bestLen = 0;
		List<String> order = SortData.COOKING_LEVEL_ORDER;
		for (int i = 0; i < order.size(); i++)
		{
			String token = order.get(i);
			if (token.length() > bestLen && nameLower.contains(token))
			{
				best = i;
				bestLen = token.length();
			}
		}
		return best;
	}

	private int compareFood(int a, int b)
	{
		String na = nameOf(a).toLowerCase(Locale.ROOT);
		String nb = nameOf(b).toLowerCase(Locale.ROOT);
		int ia = foodRank(na);
		int ib = foodRank(nb);
		if (ia != ib)
		{
			return Integer.compare(ia, ib);
		}
		return na.compareTo(nb);
	}

	private int foodRank(String nameLower)
	{
		Integer r = foodOrderIndex.get(nameLower);
		if (r != null)
		{
			return r;
		}
		// Partial match for variants ("Cooked karambwan" vs "Cooked karambwan (uncooked)").
		for (Map.Entry<String, Integer> e : foodOrderIndex.entrySet())
		{
			if (nameLower.startsWith(e.getKey()))
			{
				return e.getValue();
			}
		}
		return Integer.MAX_VALUE;
	}

	private int compareRunes(int a, int b)
	{
		return Integer.compare(runeRank(nameOf(a)), runeRank(nameOf(b)));
	}

	private int runeRank(String name)
	{
		String n = name.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Integer> e : runeOrderIndex.entrySet())
		{
			if (n.startsWith(e.getKey() + " rune"))
			{
				return e.getValue();
			}
		}
		return Integer.MAX_VALUE;
	}

	private int compareComboRunes(int a, int b)
	{
		return Integer.compare(comboRuneRank(nameOf(a)), comboRuneRank(nameOf(b)));
	}

	private int comboRuneRank(String name)
	{
		String n = name.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Integer> e : comboRuneOrderIndex.entrySet())
		{
			if (n.startsWith(e.getKey() + " rune"))
			{
				return e.getValue();
			}
		}
		return Integer.MAX_VALUE;
	}

	// ── Brief #76 (revised): per-gem-family row layout for Gems section ──
	//
	// Each gem owns a row: Uncut X → X → X ring → X necklace → X amulet
	// → X bracelet. Precious gems (gold-bar jewellery) come first by
	// crafting level, then semi-precious. Family rank determines row
	// order; sub-rank determines within-row order.

	private static final Map<String, Integer> GEM_FAMILY_RANK = Map.ofEntries(
		// Precious (gold-bar) gems, ascending crafting level.
		Map.entry("Sapphire",     1),
		Map.entry("Emerald",      2),
		Map.entry("Ruby",         3),
		Map.entry("Diamond",      4),
		Map.entry("Dragonstone",  5),
		Map.entry("Onyx",         6),
		Map.entry("Zenyte",       7),
		// Semi-precious (silver-bar) gems.
		Map.entry("Opal",         8),
		Map.entry("Jade",         9),
		Map.entry("Red topaz",   10)
	);

	/** Returns family rank for a gem family name. Items without setName
	 *  (or with an unknown family) sort to the end. */
	private static int gemFamilyRank(String family)
	{
		if (family == null) return 99;
		Integer r = GEM_FAMILY_RANK.get(family);
		return r != null ? r : 50;
	}

	/** Within-family ordering: uncut → cut → ring → necklace → amulet →
	 *  bracelet. Anything else (jewellery variants, charged orbs, etc.)
	 *  sorts to the end of the row. */
	private static int gemSubRank(String name)
	{
		String n = name.toLowerCase(Locale.ROOT);
		if (n.startsWith("uncut ")) return 0;
		// Bare cut gem name: no slot keyword, no "uncut " prefix.
		if (!n.contains(" ring") && !n.contains(" necklace")
			&& !n.contains(" amulet") && !n.contains(" bracelet")
			&& !n.contains("dragon necklace"))
		{
			return 1;
		}
		if (n.contains(" ring"))     return 2;
		if (n.contains(" necklace") || n.contains("dragon necklace")) return 3;
		if (n.contains(" amulet"))   return 4;
		if (n.contains(" bracelet")) return 5;
		return 9;
	}

	/** Brief #76 (revised): comparator for the crafting Gems section.
	 *  Sort by gem family → within-family sub-rank → name. The layout
	 *  builder then injects a row break between families. */
	private int compareGems(int a, int b)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		ItemMeta ma = meta.get(a);
		ItemMeta mb = meta.get(b);
		int fa = gemFamilyRank(ma != null ? ma.setName : null);
		int fb = gemFamilyRank(mb != null ? mb.setName : null);
		if (fa != fb) return Integer.compare(fa, fb);

		int sa = gemSubRank(nameOf(a));
		int sb = gemSubRank(nameOf(b));
		if (sa != sb) return Integer.compare(sa, sb);
		return nameOf(a).compareToIgnoreCase(nameOf(b));
	}

	private int compareWeapons(int a, int b)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		ItemMeta ma = meta.get(a);
		ItemMeta mb = meta.get(b);
		int ra = ma != null ? SortData.weaponClassRank(ma.weaponClass) : Integer.MAX_VALUE;
		int rb = mb != null ? SortData.weaponClassRank(mb.weaponClass) : Integer.MAX_VALUE;
		if (ra != rb)
		{
			return Integer.compare(ra, rb);
		}
		// Same class → tier descending.
		int ta = ma != null ? ma.tier : 0;
		int tb = mb != null ? mb.tier : 0;
		if (ta != tb)
		{
			return Integer.compare(tb, ta);
		}
		return nameOf(a).compareTo(nameOf(b));
	}

	/** Brief #66: simple-mode Armor section ordering. Sort by slot rank
	 *  (head → body → legs → hands → feet), then by tier descending, then
	 *  by name as a final tiebreaker. Items without slot metadata bucket
	 *  at the end. */
	private int compareArmorSlotThenTier(int a, int b)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		ItemMeta ma = meta.get(a);
		ItemMeta mb = meta.get(b);
		int sa = ma != null ? SortData.armorSlotRank(ma.slot) : 99;
		int sb = mb != null ? SortData.armorSlotRank(mb.slot) : 99;
		if (sa != sb)
		{
			return Integer.compare(sa, sb);
		}
		int ta = ma != null ? ma.tier : 0;
		int tb = mb != null ? mb.tier : 0;
		if (ta != tb)
		{
			return Integer.compare(tb, ta);
		}
		return nameOf(a).compareTo(nameOf(b));
	}

	/**
	 * Brief #68: universal default sort. Wearables lead by slot rank
	 * (head → body → legs → hands → feet → cape → neck → ring → weapon →
	 * shield → ammo) so any outfit/equipment section across all tabs ends
	 * up in head-to-toe order. Within the same slot, sort by tier
	 * descending, then alphabetically. Non-wearables (slot=null) get
	 * slot rank 999 and naturally sit after the wearables in the section.
	 */
	private int compareByTierDescThenName(int a, int b)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		ItemMeta ma = meta.get(a);
		ItemMeta mb = meta.get(b);
		int sa = ma != null ? SortData.slotRank(ma.slot) : 999;
		int sb = mb != null ? SortData.slotRank(mb.slot) : 999;
		if (sa != sb)
		{
			return Integer.compare(sa, sb);
		}
		int ta = ma != null ? ma.tier : 0;
		int tb = mb != null ? mb.tier : 0;
		if (ta != tb)
		{
			return Integer.compare(tb, ta);
		}
		return nameOf(a).compareTo(nameOf(b));
	}

	private String nameOf(int itemId)
	{
		try
		{
			String n = itemManager.getItemComposition(itemId).getName();
			return n != null ? n : "";
		}
		catch (Exception e)
		{
			return "";
		}
	}

	// ── Brief #75: fletching per-section sort ──────────────────────────────

	/** Fletching tier resolver. Combines wood-tier ranks (for bows, stocks,
	 *  arrow shafts) and metal-tier ranks (for finished ammo, tips). Higher
	 *  number = higher tier. Returns 0 if no token matches — those items
	 *  sort to the front under the ASC ordering. */
	private static int fletchingTier(String name)
	{
		String n = name.toLowerCase(Locale.ROOT);
		// Wood tiers first — bow / stock / shaft names lead with wood token.
		if (n.contains("redwood")) return 60;
		if (n.contains("magic ") || n.startsWith("magic")) return 50;
		if (n.contains("yew ")   || n.startsWith("yew"))   return 40;
		if (n.contains("maple ") || n.startsWith("maple")) return 30;
		if (n.contains("willow ") || n.startsWith("willow")) return 20;
		if (n.contains("oak ")   || n.startsWith("oak"))   return 10;
		// Metal tiers for ammo, tips, etc. Specials get a sentinel above
		// dragon so they sort to the end of crossbow/dart progressions.
		if (n.contains("zaryte"))   return 95;
		if (n.contains("armadyl"))  return 90;
		if (n.contains("dragon "))  return 80;
		if (n.contains("amethyst")) return 75;
		if (n.contains("rune ") || n.contains("runite "))   return 70;
		if (n.contains("adamant ")) return 60;
		if (n.contains("mithril "))  return 50;
		if (n.contains("broad"))    return 45;
		if (n.contains("steel "))   return 30;
		if (n.contains("iron "))    return 20;
		if (n.contains("bronze "))  return 10;
		return 0;
	}

	/** Per-section sub-rank — keeps related items adjacent inside a
	 *  fletching section in the order players think about them. Lower
	 *  rank = earlier in the section. The combination of section + this
	 *  sub-rank + tier ASC produces the brief #75 layout. */
	private static int fletchingSubRank(String name, String section)
	{
		String n = name.toLowerCase(Locale.ROOT);
		switch (section)
		{
			case "Tools":
				if (n.contains("knife")) return 0;
				if (n.contains("fletching cape")) return 1;
				if (n.contains("fletching hood")) return 2;
				return 9;
			case "Feathers":
				if (n.contains("feather") && !n.contains("string")) return 0;
				if (n.contains("crossbow string")) return 3;
				if (n.contains("magic string")) return 2;
				if (n.contains("bow string")) return 1;
				return 9;
			case "Arrows":
				if (n.contains("arrow shaft")) return 0;
				if (n.contains("headless arrow")) return 1;
				return 2;   // finished arrows
			case "Bows":
				// Brief #75: within each wood tier, sort
				//   shortbow (u) → shortbow → longbow (u) → longbow
				{
					boolean lb = n.contains("longbow") || n.contains("comp bow")
						|| n.contains("composite bow");
					boolean u = n.contains("(u)") || n.contains("unstrung");
					int base = lb ? 2 : 0;
					return base + (u ? 0 : 1);
				}
			case "Crossbows":
				// Brief #75: within tier, stock → unstrung crossbow → finished.
				if (n.contains("stock")) return 0;
				if (n.contains("(u)") || n.contains("unstrung")) return 1;
				return 2;
			case "Bolts":
				// tip → unfinished → finished plain → gem-tipped variant.
				if (n.contains("bolt tip") || n.endsWith(" bolt tips")) return 0;
				if (n.contains("unfinished bolt") || n.contains("unfinished broad")) return 1;
				// Gem-tipped bolts contain a gem keyword (sapphire/ruby/etc.)
				// AND the word "bolts". Group them after plain finished bolts.
				if (n.contains("sapphire") || n.contains("emerald")
					|| n.contains("ruby") || n.contains("diamond")
					|| n.contains("dragonstone") || n.contains("onyx")
					|| n.contains("opal") || n.contains("jade")
					|| n.contains("pearl") || n.contains("topaz"))
				{
					return 3;
				}
				return 2;
			case "Darts":
				if (n.contains("dart tip")) return 0;
				return 1;
			case "Javelins":
				if (n.contains("javelin head") || n.contains("javelin tip")
					|| n.contains("javelin shaft")) return 0;
				return 1;
			default:
				return 0;
		}
	}

	/** Brief #75 (revised) + Brief #88: fletching within-section comparator.
	 *  Tier component is DESC throughout so rows render strongest-LEFT
	 *  (closest to the tab rail). Sub-rank / process-flow ordering is
	 *  ascending and unaffected — that's structural, not a tier ranking.
	 *
	 *  Bows: primary = wood tier DESC, secondary = sub-rank
	 *    (shortbow-u → shortbow → longbow-u → longbow), tertiary = name.
	 *  Crossbows: primary = tier DESC (specials at the left via tier sentinel),
	 *    secondary = sub-rank (stock → unstrung xbow → finished), name.
	 *  Arrows: primary = sub-rank (shaft → headless → finished), then within
	 *    finished by tier DESC, name.
	 *  Darts / Javelins: primary = tier DESC, secondary = sub-rank
	 *    (tip → finished / head → finished), name.
	 *  Bolts: primary = sub-rank (tip → unf → finished → gem-tipped),
	 *    secondary = tier DESC within sub-rank, name.
	 *  Feathers: primary = sub-rank (feathers → bowstring → magic string →
	 *    crossbow string), name.
	 *  Tools / Logs / Arrowheads / Misc: sub-rank then tier DESC then name.
	 */
	private int compareFletching(int a, int b, String section)
	{
		String na = nameOf(a);
		String nb = nameOf(b);

		int sra = fletchingSubRank(na, section);
		int srb = fletchingSubRank(nb, section);
		int ta = fletchingTier(na);
		int tb = fletchingTier(nb);

		// Bows + Crossbows + Darts + Javelins: tier-first within section.
		boolean tierFirst = "Bows".equals(section)
			|| "Crossbows".equals(section)
			|| "Darts".equals(section)
			|| "Javelins".equals(section);

		if (tierFirst)
		{
			// Brief #88: tier DESC → strongest-LEFT.
			if (ta != tb) return Integer.compare(tb, ta);
			if (sra != srb) return Integer.compare(sra, srb);
			return na.compareToIgnoreCase(nb);
		}

		// Arrows + Bolts + Feathers: sub-rank-first (shaft/tip/feather before
		// finished etc.), then tier DESC within sub-rank.
		boolean subRankFirst = "Arrows".equals(section)
			|| "Bolts".equals(section)
			|| "Feathers".equals(section);

		if (subRankFirst)
		{
			if (sra != srb) return Integer.compare(sra, srb);
			// Brief #88: tier DESC within the sub-rank bucket.
			if (ta != tb) return Integer.compare(tb, ta);
			return na.compareToIgnoreCase(nb);
		}

		// Tools (3 items, fixed order via sub-rank), Logs / Arrowheads /
		// Misc fletching: sub-rank then tier DESC then name.
		if (sra != srb) return Integer.compare(sra, srb);
		// Brief #88: tier DESC → strongest-LEFT.
		if (ta != tb) return Integer.compare(tb, ta);
		return na.compareToIgnoreCase(nb);
	}

	// ── Brief #75: cosmetics set-then-slot sort ───────────────────────────

	private static final Map<String, Integer> SLOT_ORDER_COSMETICS = Map.ofEntries(
		Map.entry("head", 1),
		Map.entry("body", 2),
		Map.entry("legs", 3),
		Map.entry("hands", 4),
		Map.entry("feet", 5),
		Map.entry("cape", 6),
		Map.entry("neck", 7),
		Map.entry("ring", 8),
		Map.entry("weapon", 9),
		Map.entry("2h", 9),
		Map.entry("shield", 10),
		Map.entry("ammo", 11)
	);

	/** Treasure-trail clue-tier rank per set_name. Lower = earlier (easy
	 *  clues first → master clues last). Sets that don't appear here get a
	 *  default rank so they sort after the explicit list. */
	private static final Map<String, Integer> TREASURE_SET_TIER = Map.ofEntries(
		Map.entry("Explorer", 1),
		Map.entry("Black (g)", 2),  Map.entry("Black (t)", 2),
		Map.entry("Adamant (g)", 3), Map.entry("Adamant (t)", 3),
		Map.entry("Heraldic 1", 4),  Map.entry("Heraldic 2", 4),
		Map.entry("Heraldic 3", 4),  Map.entry("Heraldic 4", 4),
		Map.entry("Heraldic 5", 4),
		Map.entry("Rune (g)", 5),    Map.entry("Rune (t)", 5),
		Map.entry("Elegant", 5),
		Map.entry("Elegant black", 5), Map.entry("Elegant white", 5),
		Map.entry("Elegant red", 5), Map.entry("Elegant purple", 5),
		Map.entry("Elegant pink", 5), Map.entry("Elegant green", 5),
		Map.entry("Elegant gold", 5), Map.entry("Elegant blue", 5),
		Map.entry("Musketeer", 5),
		Map.entry("Gilded", 6),
		Map.entry("3rd age melee", 7),
		Map.entry("3rd age range", 7),
		Map.entry("3rd age mage", 7),
		Map.entry("3rd age druidic", 7)
	);

	private static final Map<String, Integer> HOLIDAY_SET_TIER = Map.ofEntries(
		Map.entry("Christmas", 1),
		Map.entry("Halloween", 2),
		Map.entry("Easter", 3),
		Map.entry("Midsummer", 4),
		Map.entry("Diwali", 5),
		Map.entry("Thanksgiving", 6),
		Map.entry("Valentine", 7),
		Map.entry("Festive", 8)
	);

	private static int cosmeticsSlotRank(String slot)
	{
		if (slot == null) return 99;
		Integer r = SLOT_ORDER_COSMETICS.get(slot.toLowerCase(Locale.ROOT));
		return r != null ? r : 99;
	}

	/** Set-rank for a (section, set_name) pair. Used as the primary cosmetics
	 *  sort key so items in the same set cluster contiguously. Items with
	 *  set_name == null get a sentinel rank that places them after all
	 *  recognised sets. */
	private static int cosmeticsSetRank(String section, String setName)
	{
		if (setName == null) return 9999;
		if ("Treasure trail sets".equals(section))
		{
			Integer r = TREASURE_SET_TIER.get(setName);
			return r != null ? r : 50;
		}
		if ("Holiday items".equals(section))
		{
			Integer r = HOLIDAY_SET_TIER.get(setName);
			return r != null ? r : 50;
		}
		// Minigame / Random event / Miscellaneous: alpha by set name
		// (the secondary key in compareCosmetics handles that).
		return 0;
	}

	/** Brief #75: cosmetics comparator. Primary key is the set's clue tier
	 *  / holiday rank, secondary is set_name alphabetical (groups by set),
	 *  tertiary is slot rank (head → body → legs → hands → feet → cape →
	 *  neck → ring → weapon → shield), final tiebreaker is name. Items
	 *  with no set_name sort after items with a set, in alpha order. */
	private int compareCosmetics(int a, int b, String section)
	{
		Map<Integer, ItemMeta> meta = SortData.itemMeta();
		ItemMeta ma = meta.get(a);
		ItemMeta mb = meta.get(b);
		String sna = ma != null ? ma.setName : null;
		String snb = mb != null ? mb.setName : null;

		int ra = cosmeticsSetRank(section, sna);
		int rb = cosmeticsSetRank(section, snb);
		if (ra != rb)
		{
			return Integer.compare(ra, rb);
		}

		String keyA = sna != null ? sna : "";
		String keyB = snb != null ? snb : "";
		int byName = keyA.compareToIgnoreCase(keyB);
		if (byName != 0)
		{
			return byName;
		}

		int slA = cosmeticsSlotRank(ma != null ? ma.slot : null);
		int slB = cosmeticsSlotRank(mb != null ? mb.slot : null);
		if (slA != slB)
		{
			return Integer.compare(slA, slB);
		}
		return nameOf(a).compareToIgnoreCase(nameOf(b));
	}

	// ── Brief #87: range tab fixed launcher→ammo rows ────────────────────

	private static final Set<String> RANGE_WEAPON_SECTIONS = Set.of(
		"Bows", "Arrows", "Crossbows", "Bolts",
		"Ballistae & javelins", "Blowpipe & darts",
		"Knives", "Morrigan's javelins", "Other throwables"
	);

	/** Range-weapon tier resolver. Wood tiers for bow progression, metal
	 *  tiers for crossbow / arrow / bolt / dart / javelin / knife
	 *  progression, plus special endgame ranks (twisted, zaryte, etc.).
	 *  Higher = stronger / later. Returns 0 for unknown so untagged items
	 *  sort to the front of the row under the ASC ordering. */
	private static int rangeTier(String name)
	{
		String n = name.toLowerCase(Locale.ROOT);
		// Special-tier endgame ranged weapons & ammo.
		if (n.contains("twisted bow"))           return 999;
		if (n.contains("bow of faerdhinen"))     return 950;
		if (n.contains("zaryte crossbow"))       return 900;
		if (n.contains("webweaver"))             return 880;
		if (n.contains("armadyl crossbow"))      return 850;
		if (n.contains("dragon hunter crossbow")) return 840;
		if (n.contains("blazing blowpipe"))      return 820;
		if (n.contains("toxic blowpipe"))        return 800;
		if (n.contains("crystal bow"))           return 780;
		if (n.contains("3rd age bow"))           return 770;
		if (n.contains("dark bow"))              return 750;
		if (n.contains("magic shortbow"))        return 60;  // wood-tier magic
		// Specialty / ballista — only used inside Ballistae row.
		if (n.contains("heavy ballista"))        return 95;
		if (n.contains("light ballista"))        return 90;
		// Wood tiers (bows / strung shortbow + longbow / crystal).
		if (n.contains("redwood"))               return 60;
		if (n.contains("magic "))                return 50;
		if (n.contains("yew "))                  return 40;
		if (n.contains("maple "))                return 30;
		if (n.contains("willow "))               return 20;
		if (n.contains("oak "))                  return 10;
		// Metal tiers (ammo + crossbows + darts + knives + javelins).
		if (n.contains("dragon "))               return 80;
		if (n.contains("amethyst"))              return 75;
		if (n.contains("rune ") || n.contains("runite ")) return 70;
		if (n.contains("adamant "))              return 60;
		if (n.contains("mithril "))              return 50;
		if (n.contains("broad"))                 return 45;
		if (n.contains("steel "))                return 30;
		if (n.contains("iron "))                 return 20;
		if (n.contains("bronze "))               return 10;
		if (n.contains("blurite"))               return 15;
		if (n.contains("silver "))               return 25;
		return 0;
	}

	/** Within-section sub-rank for the Ballistae & javelins row and the
	 *  Blowpipe & darts row — launcher items lead the row, then ammo
	 *  follows. Other range sections return 0 (sub-rank doesn't fire). */
	private static int rangeSubRank(String name, String section)
	{
		String n = name.toLowerCase(Locale.ROOT);
		switch (section)
		{
			case "Ballistae & javelins":
				if (n.contains("light ballista")) return 0;
				if (n.contains("heavy ballista")) return 1;
				return 2;  // javelins
			case "Blowpipe & darts":
				if (n.contains("blowpipe")) return 0;
				return 1;  // darts
			default:
				return 0;
		}
	}

	/** Brief #87 + Brief #88: comparator for the nine range-weapon sections.
	 *  Sub-rank (launcher → ammo) primary in the Ballistae / Blowpipe rows
	 *  — that's a structural launcher-first convention, ASC. Tier component
	 *  is DESC so the strongest item in each row renders LEFT, closest to
	 *  the tab rail. Combat tabs across the plugin standardise on
	 *  strongest-left; herblore / runecraft / crafting Gems intentionally
	 *  go low-tier-first because those are training rows where the player
	 *  reaches for the low-level item most often. */
	private int compareRangeWeapon(int a, int b, String section)
	{
		String na = nameOf(a);
		String nb = nameOf(b);
		int sra = rangeSubRank(na, section);
		int srb = rangeSubRank(nb, section);
		if (sra != srb) return Integer.compare(sra, srb);
		int ta = rangeTier(na);
		int tb = rangeTier(nb);
		// Brief #88: tier DESC → strongest-LEFT.
		if (ta != tb) return Integer.compare(tb, ta);
		return na.compareToIgnoreCase(nb);
	}

	/** Emit every owned variant in {@code iid}'s variation family at
	 *  consecutive positions (base item first). Falls back to the canonical
	 *  id itself if the display map has no entry. Returns the next free pos. */
}

# Sorted tabs

- **Package:** `com.ultimatebank.feature.sortedtabs`
- **Status:** see docs/roadmap.md
- **Sources:** Bank Skill Sorting / "Auto Bank Sorter" by Frailrain (https://github.com/Frailrain/Bank-Skill-Sorting,
  Hub commit `7fe2106`), BSD-2. Item data (`item-meta.json`, tab membership, section orders, slayer tasks),
  layout/sorting logic and the section-title overlay. See docs/attribution.md.

## Goal
Auto-sorted, skill-based bank tag tabs with titled sections (like Bank Skill Sorting), **but the user can
customize them and edits are never overwritten**: add/remove items, move items between sections, rename/add/delete
sections, two levels of titles (section › sub-section), and their own tabs.

Decisions (2026-10-18): start from the skill tabs and customize; edit both in-bank (shift-right-click) and in a
sidebar panel; two title levels; items may live in multiple tabs; variant-aware; dynamic slayer tab (last phase).

## Architecture
```
defaults (ported data, read-only)        user edits (saved, ours)
  tab list + icons                          per tab: section adds/renames/deletes/order,
  item -> section per tab (item-meta)       item moves/adds/removals; custom tabs
  section order per tab
  sorting rules per section
            \                                /
             +---> TabResolver (pure) ------+
                   = ordered sections [title, level, items]
                            |
              LayoutBuilder: owned items only, variant-aware,
              a blank header row before each section
                            |
     core bank tags (TagManager tags + LayoutManager layout)   +   title overlay
```
- **Edits win, defaults keep flowing:** edits are stored as a diff over the defaults, so item-data updates
  (new game items) still appear in default sections unless the user removed or moved them.
- **Tab names** use the suffix ` (ub)` (e.g. `melee (ub)`), so we never touch user tags or Bank Skill Sorting's
  `(auto)` tabs. Bank Tags lower-cases tab names.
- **Perf:** layouts rebuild only when the bank's contents fingerprint changes, and only for the tab being viewed
  (lessons from upstream issues #5, #12, #13).

## Phases
1. **Port + display:** data, sorting, tags and layouts for every skill tab with section titles drawn; per-tab
   toggles; reset. Edits model + resolver in place (unit tested) but no editing UI yet.
2. **In-bank editing:** shift-right-click an item › Move to section ›, Remove from tab; on a title › Rename,
   Add section above/below, Add sub-section, Delete section, Add item (search).
3. **Sidebar editor:** tree of tabs › sections › sub-sections › items with add/rename/delete/reorder, new custom
   tabs, reset tab to default.
4. **Dynamic slayer tab:** port SlayerLoadoutBuilder + slayer-tasks.json.

## Implementation (phase 1)
- `data/DefaultTabs` + `default-tabs.json`: 22 tabs (id, icon, items), converted from upstream `SkillBankData.java` by
  `tools/convert_bss_tabs.py` (rerun when upstream data changes).
- `data/SortData`, `data/ItemMeta`, `item-meta.json`: copied from upstream (section per item per tab, tiers,
  requirements, hand-authored orders, `TAB_SECTIONS`, `TWO_ZONE_TABS`).
- `SectionSorter`: upstream SkillBankLayoutBuilder's sorting/partition code only (`sort`, `partition`).
- `TabEdits` (diff over defaults, JSON in config `ultimatebank.sortedTabsEdits.<tabId>`) + `TabResolver` (pure,
  `TabResolverTest`), giving ordered `ResolvedSection`s (id, title, level, sortDomain, items).
- `SortedLayoutBuilder`: owned items only, variant-aware, each section on a new row with a blank title row before
  it; a sub-section's parent title is shown even when the parent has no items of its own; combat tabs' lower-tier
  items go to a "Lower tier" group at the end.
- `SortedTabsFeature`: **virtual tags** (`TagManager.registerTag`) instead of upstream's thousands of real item
  tags; adds `<tab> (ub)` to the Bank Tags tab list with an icon (tracked in `sortedTabsCreated`, removed again
  on disable). Rebuilds a tab's layout when it's viewed and the bank fingerprint changed (one tick deferred).
- **Published tabs** (`core/ExtraTabs`): other features can publish a sectioned tab that shows up as an extra
  `(ub)` tab. Storage savings publishes `storable` (one section per storage location, most bank slots first; the
  plain `storable` tab hides while Sorted tabs is displaying). Published sections are shown as-is (no sorting or
  edits). Toggle: `sortedTabsStorable`.
- `SortedTabsOverlay`: titles + divider lines from row positions (bold section titles, smaller indented orange
  sub-section titles).

## Config
Section "Sorted tabs": `sortedTabsEnabled` (false), `sortedTabsTitles` (true), `sortedTabsLowerTier` (true),
`sortedTabsLowerTierCount` (3). Section "Sorted tabs: which tabs": one toggle per tab, all on
(`sortedTabsMelee`, ..., `sortedTabsWoodcuttingFiremaking`; key = "sortedTabs" + CamelCase id).

## In-game test checklist (phase 1)
- [ ] Enable Sorted tabs: 23 `... (ub)` tabs appear at the end of the tag tab list with icons, `storable (ub)` last
- [ ] `storable (ub)`: a titled section per storage location, biggest first; the plain `storable` tab is gone;
      ignoring an item or toggling a location updates it; disabling Sorted tabs brings the plain `storable` tab back
- [ ] Open `melee (ub)`: gear grouped under titled sections (Weapons, Head, ...) with divider lines; strongest
      gear first; low-level gear under "Lower tier" at the bottom
- [ ] Cooking: raw/cooked/burnt fish rows; Herblore: herbs in level order; Cosmetics: one row per set
- [ ] Degraded Barrows / charged jewellery sit where the base item would
- [ ] Deposit an item that belongs in the open tab: it pops into its sorted spot within a tick
- [ ] Scroll a long tab: titles move with the rows and never draw outside the bank area
- [ ] Turn off "Section titles": no blank rows; turn off "Lower tier at bottom": low gear stays in its section
- [ ] Turn off one tab in "which tabs": it disappears from the tab list; turn it back on: it returns
- [ ] Disable Sorted tabs: all `(ub)` tabs, icons and layouts are gone; your own tabs untouched
- [ ] No noticeable hitch when opening/closing the bank

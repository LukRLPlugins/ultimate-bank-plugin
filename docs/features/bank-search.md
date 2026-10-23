# Ultimate Bank Search

- **Package:** `com.ultimatebank.feature.banksearch` (+ shared `core/snapshots`)
- **Formerly:** "Where's my stuff" (renamed in #21 before release; keys were never released).
- **Status:** done (#21, merged in #33); ships in v1.2.0. Snapshots in `core/snapshots` are always on (Storage
  savings uses them too). Ships **on for new installs, off for existing ones** (`NEW_FEATURE_TOGGLES`).
- **Sources:** Where's My Stuff / Bank POH Search by Wozak-1 (https://github.com/Wozak-1/bank-poh-searcher, Hub
  commit `95ec7f4`), BSD-2. Bank value (#10): ideas from Bank Value by spudjb
  (https://github.com/spudjb/runelite-bank-value, Hub commit `cdf25aa`), BSD-2, folded in here instead of a
  separate panel (no code copied). See docs/attribution.md.

## Behaviour
- **Snapshots (core, always on):** one snapshot per visit to the bank or a POH costume room storage (magic
  wardrobe, toy box, cape rack, treasure chest, armour case, fancy dress box, bookcase). While it's open the visit's
  snapshot is kept up to date, so the previous snapshot vs the latest shows everything taken or deposited in that
  visit. A visit with no net change only refreshes the last snapshot's time. Saved per account, last N kept
  ("Visits kept", default 5).
- **Container storages (#41):** the POH pet house, seed vault, tackle box, huntsman's kit and forestry kit are read
  from their item container while their interface is open (`core/snapshots/ContainerStorage`), with the same
  one-snapshot-per-visit rule. Still missing (#41 follow-ups): tool leprechaun and fossil storage (varbits), and
  the steel key ring, master scroll book, spice rack, Flamtaer bag, Nightmare Zone, Elnock Inquisitor and
  mysterious stranger (to research).
- **Sidebar panel** (magnifier icon), with one search box over two tabs:
  - **Items:** a collapsible section per location: name (POH in orange), item count, total value, "seen X ago"
    (orange/red once old/stale). A search opens every section it matches; searching a location name shows all its
    items. Sort by Value (stack value), Quantity or Name.
  - **Bank value (#10):** a Total card above the sections: item count and value of everything recorded, or of what
    matches the search (with its share of everything). Each row's tooltip shows its share of everything you own.
    Value items at **GE price** (high alch for untradeables) or **High alch**, from the box next to the sort (saved
    as `bankSearchValueType`). Coins and platinum tokens count at face value either way. Core's bank title already
    shows the open tab's GE/HA value, so there's no bank overlay.
  - **History:** one card per visit (consecutive snapshots of a location), newest first across all locations: the
    location, when, the net value change, and each item taken out (red −) or put in (green +). A location's first
    snapshot isn't a visit.
  - Rows are Loot Tracker style: icon, name, quantity over value. Up to 200 per section/visit, then "search to
    narrow". Everything fits the sidebar width: long names are cut short with "..." (full name on hover).
- **Storage savings uses the snapshots:** works from the saved bank before you open it this session ("from your
  bank as last seen X ago"); its tooltip shows how many copies a snapshotted storage already holds ("(2 stored)").

Dropped for v1 (follow-ups): equipment stat comparison, STASH units in the search.

## Config
Section "Bank Search: General": `bankSearchEnabled` (true for new installs; set false once for existing installs by
`core/FeatureRollout`), `bankSearchSnapshotCount` ("Visits kept", 5, 2–20, also used by core snapshots),
`bankSearchWarnHours` (24), `bankSearchStaleHours` (168), `bankSearchValueType` (GE price / High alch, GE).
Storage savings: `storageSavingsUseSnapshots` (true).
Stored per RS profile: `ultimatebank.snapshots.<location>` (JSON list of `{quantities, takenAt}`, oldest first).

## Implementation
- `core/snapshots/StorageSnapshots`: registered on the EventBus by `UltimateBankPlugin` (not a feature, so it records
  even with both features off). Bank: `ItemContainerChanged` → read next tick, only while `Bankmain.ITEMS` exists;
  placeholders (quantity 0) skipped. POH: `WidgetLoaded` for `InterfaceID.POH_COSTUMES` → scan after 2 ticks, up to
  6 tries, then rescan the tick after each inventory change while it's open. `SnapshotHistory` (pure, tested): the
  first change in a visit appends, later ones replace that entry; unchanged contents only refresh the timestamp. A
  visit ends when `Bankmain.ITEMS` / `PohCostumes.UNIVERSE` is gone, on logout, or on an account switch.
- `core/snapshots/PohStorageScanner`: storage from interface text (`SnapshotLocation.fromInterfaceText`), stored
  items = item widgets under `PohCostumes.ITEMS` with opacity 0. Logs `PohCostumes.TIER` text (for #14).
- `feature/banksearch`: `SnapshotDiff`, `VisitLog` (history → visits) and `StuffGroups` (group/filter/sort) are
  pure; `StuffRow`/`StuffVisit` are display rows; `BankSearchPanel` (sections, `MaterialTabGroup`, a `JList`
  renderer per section); `BankSearchFeature` rebuilds on snapshot changes (client thread for names/prices).
- Tests: `SnapshotDiffTest`, `VisitLogTest`, `StuffGroupsTest`, `SnapshotJsonTest`, `SnapshotHistoryTest`.

## In-game test checklist
- [ ] Existing install (the dev profile has v1.x data): first launch has the panel **off** and the setting unticked.
      Tick it: the magnifier button appears; restart: still on. A fresh profile starts with it **on**.
- [ ] Items tab: the Bank section shows its item count, value and "seen 0 minutes ago"; click to open it: rows have
      icons, quantities and values; click again to close
- [ ] Open each POH storage: it gets its own section (orange name)
- [ ] Search "shark": only sections with sharks, opened automatically; search "toy box": everything in it
- [ ] Sort by Name / Value / Quantity reorders rows
- [ ] Total card: item count and value of everything; search "rune": "Matching" with the share of everything
- [ ] Hover a row: tooltip shows its share of everything you own
- [ ] Switch to High alch: values, totals and History change (coins unchanged); restart: still High alch; the
      setting "Value items at" in Bank Search: General matches
- [ ] Bank visit: withdraw 3 different items and deposit 1, close the bank. History has one "Bank · 0 minutes ago"
      card with those changes (red −, green +) and the net value
- [ ] Open and close the bank without changes: no new card
- [ ] Take an item out of the toy box: a toy box card with −1
- [ ] Open the seed vault: a "Seed vault" section with your seeds; search a seed name: it's found there. Withdraw a
      seed and close the vault: a seed vault History card with the change
- [ ] Same for the tackle box, huntsman's kit, forestry kit (open each) and the POH pet house (orange)
- [ ] Bank Savings: a banked seed that's also in the vault says "Seed vault (N stored)" on hover
- [ ] Set "Visits kept" to 2: after the next visit, at most one card per location
- [ ] Large bank: scrolling stays smooth
- [ ] Turn off "Ultimate Bank Search": button disappears; Storage savings unaffected (saved bank, "(N stored)")

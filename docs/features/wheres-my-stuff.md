# Where's my stuff

- **Package:** `com.ultimatebank.feature.wheresmystuff` (+ shared `core/snapshots`)
- **Status:** search panel **hidden** (not ready for release, see #21); snapshots in `core/snapshots` stay on for
  Storage savings
- **Sources:** Where's My Stuff / Bank POH Search by Wozak-1 (https://github.com/Wozak-1/bank-poh-searcher, Hub
  commit `95ec7f4`), BSD-2. See docs/attribution.md.

## Behaviour
- **Snapshots (core, always on):** the bank is recorded whenever it changes while open; each POH costume room
  storage (magic wardrobe, toy box, cape rack, treasure chest, armour case, fancy dress box, bookcase) when you open
  it. Saved per account, last N kept (default 5).
- **Sidebar panel** (magnifier icon): search by item name or location across bank + POH; each row shows icon,
  quantity, location and stack value (GE price, high alch for untradeables). Sort by Name, Value, or **Recently
  changed** (items whose quantity changed since the previous snapshot of that location, biggest value change
  first, green +/red -). A freshness line per location turns orange/red when old/stale.
- **Storage savings uses the snapshots:** works from the saved bank before you open it this session ("from your
  bank as last seen X ago"); its tooltip shows how many copies a POH storage already holds ("(2 stored)").

Dropped for v1 (follow-ups): equipment stat comparison, STASH units in the search.

## Config
Section "Where's my stuff": `wheresMyStuffEnabled` (true), `wheresMyStuffSnapshotCount` (5, 2–20),
`wheresMyStuffWarnHours` (24), `wheresMyStuffStaleHours` (168).
Storage savings: `storageSavingsUseSnapshots` (true).
Stored per RS profile: `ultimatebank.snapshots.<location>` (JSON list of `{quantities, takenAt}`, oldest first).

## Implementation
- `core/snapshots/StorageSnapshots`: registered on the EventBus by `UltimateBankPlugin` (not a feature, so it records
  even with both features off). Bank: `ItemContainerChanged` → read next tick, only while `Bankmain.ITEMS` exists;
  placeholders (quantity 0) skipped. POH: `WidgetLoaded` for `InterfaceID.POH_COSTUMES` → scan after 2 ticks, up to
  6 tries. An unchanged snapshot only refreshes its timestamp; a changed one is appended.
- `core/snapshots/PohStorageScanner`: storage from interface text (`SnapshotLocation.fromInterfaceText`), stored
  items = item widgets under `PohCostumes.ITEMS` with opacity 0. Logs `PohCostumes.TIER` text (for #14).
- `feature/wheresmystuff`: `SnapshotDiff` (pure), `StuffRow`, `WheresMyStuffPanel` (JList, max 500 rows shown),
  `WheresMyStuffFeature` (rebuilds rows on snapshot changes, client thread for names/prices).
- Tests: `SnapshotDiffTest` (diff + location text), `SnapshotJsonTest` (JSON round trip).

## In-game test checklist
- [ ] Open the bank: magnifier panel shows "Bank: seen 0 minutes ago" and your items; search by name works
- [ ] Open each POH storage in your costume room: it appears in the freshness list and its items are searchable
      (search "toy box" lists everything in it)
- [ ] Sort by Value: most valuable stacks first. Deposit/withdraw something, reopen the bank, sort by
      Recently changed: it shows with a green +/red − amount
- [ ] Restart the client and log in without opening the bank: panel still has your data; Storage savings panel says
      "From your bank as last seen …" and markers/ranking work
- [ ] Bank a copy of something already in your toy box: still suggested for the toy box, tooltip says "(N stored)",
      Storage savings panel counts it under "already have copies stored in your POH"
- [ ] Turn off "Use saved snapshots": Storage savings waits for the bank again; no "(N stored)" notes
- [ ] Turn off "Where's my stuff": panel button disappears; Storage savings unaffected


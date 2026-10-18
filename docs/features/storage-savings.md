# Storage savings

- **Package:** `com.ultimatebank.feature.storagesavings`
- **Status:** see docs/roadmap.md
- **Sources:** Wasted Bank Space by Riley McGee (https://github.com/mcgeer/WastedBankSpace, Hub commit `1cb3b28`),
  plus unmerged PRs: STASH units and tag tab by Brettgod1355 (#136, #146, branch `stash-units` @ `e372558`),
  data fixes #124, #125, #132, #133. All BSD-2. See docs/attribution.md.

## Behaviour
After the bank has been opened this session:
- **Bank marker:** a corner triangle (configurable color) on each storable bank item. Hovering lists every enabled
  place it can go, with at most 3 STASH units plus "...and N more".
- **Sidebar panel:** ranks storage locations by how many bank slots they would free, most first. POH builds are
  orange. Click a location to list its items. This answers "which upgrade frees the most space".
- **`storable` tag tab:** a virtual bank tag (core `TagManager.registerTag`) showing only flagged items. The tab is
  added to the Bank Tags tab list and removed again when the option or feature is turned off. A user's own tab
  with the same name is never removed.
- **Ignore list:** config text box (names, IDs, `*` wildcards; case and space insensitive; a shared name ignores
  all items with that name), plus shift-right-click on a marked bank item › "Ignore storable" / "Unignore storable".
  Unignore only removes exact entries, and wildcards stay.
- **Never flag best-in-slot** (default on): skips items any location marks as BIS (capes, Ancestral, ...).
- **STASH units** (default off): flags items that fit a STASH unit. With "Only built, empty STASHes" (default on)
  it only counts units that are built and not yet filled. Filled state is accurate after the POH STASH chart or
  Watson's noticeboard has been opened once, and deposits/withdrawals keep it current after that.

Differences from the source plugin: the ranking is new; the marker is drawn (no PNG assets); no marker on tag tab
icons (upstream #108); tooltip names every location (upstream #143); item lists use gameval IDs and include the
data-fix PRs; no persistence of the bank between sessions (not requested).

## Config
Section "Storage savings":
| Key | Type | Default |
|---|---|---|
| `storageSavingsEnabled` | boolean | true |
| `storageSavingsMarker` | boolean | true |
| `storageSavingsMarkerColor` | Color | (255,200,0) |
| `storageSavingsTagTab` | boolean | true |
| `storageSavingsPanel` | boolean | true |
| `storageSavingsKeepBis` | boolean | true |
| `storageSavingsIgnored` | String | "" |
| `storageSavingsStash` | boolean | false |
| `storageSavingsStashBuiltOnly` | boolean | true |

Section "Storage savings: locations": one `storageSavings<Location>` toggle per `StorageLocation`, all on except
armour case, cape rack and pet house.

Internal (not shown): `storageSavingsTagTabCreated` (global), `storageSavingsStashFilled` and
`storageSavingsStashChart` (per RuneScape profile).

## Implementation
- `StorageSavingsFeature`: event handling, bank scan (placeholders skipped), `recompute()` builds the flagged map
  and ranking and publishes them to `StorageSavingsState`.
- `data/StorageLocation` + `data/*Items`: item lists (generated from upstream by converting legacy `ItemID` to
  gameval by numeric value). **To add an item, edit the matching `*Items` class.**
- `stash/`: `StashUnit` (data), `StashTracker` (built/filled detection via game script `WATSON_STASH_UNIT_CHECK`,
  STASH chart script args, the `HH_INV` container and deposit/withdraw chat messages). Ported almost unchanged.
- `StorageSavingsOverlay` (WidgetItemOverlay on `Bankmain.ITEMS` only), `StorageSavingsTagTab`, `StorageSavingsPanel`.
- Events: `ItemContainerChanged`, `ConfigChanged`, `MenuOpened`, `RuneScapeProfileChanged`, and only while STASH is
  on, `GameTick`, `GameStateChanged`, `VarbitChanged`, `ScriptPreFired`, `MenuOptionClicked`, `ChatMessage`.
- Tests: `IgnoreListTest`, `data/StorageLocationTest`.

## In-game test checklist
- [ ] Open the bank. Storable items get a yellow corner marker, and hovering lists where they can go
- [ ] Sidebar chest icon › panel ranks locations with slot counts; POH ones orange; click to expand/collapse
- [ ] Bank shows a `storable` tag tab (treasure chest icon); opening it shows only marked items
- [ ] Shift-right-click a marked item › "Ignore storable": marker disappears, item appears in the config text box;
      shift-right-click again › "Unignore storable" brings it back
- [ ] Type `*cape*` in Ignored items: all storable capes lose their marker
- [ ] Toggle a location (e.g. POH toy box) off/on: markers and ranking update
- [ ] Turn on Armour case / Cape rack: many more items flagged; BIS capes stay unflagged while "Never flag
      best-in-slot" is on
- [ ] STASH units on: after a game tick, STASH items you've built-but-not-filled are flagged; open the POH STASH
      chart once and check filled units drop out
- [ ] Turn the feature off: markers, panel icon and `storable` tab all disappear; turn on again: they return
- [ ] Edge: placeholders are never marked; bank search and other tag tabs still show markers only on items

## Open questions
- Persist the last bank snapshot so the panel works before opening the bank? (Declined for now.)
- Should "POH costume room already holds this" (upstream PR #139) live here or in the Costume Room Highlighter feature?

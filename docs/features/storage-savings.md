# Ultimate Bank Savings (Storage savings)

- **Package:** `com.ultimatebank.feature.storagesavings`
- **Shown as:** "Ultimate Bank Savings" (panel, sidebar button; settings sections "Bank Savings: …"). Code and config keys keep the
  original `storageSavings` name (keys are released, so they never change).
- **Status:** see docs/roadmap.md
- **Sources:** Wasted Bank Space by Riley McGee (https://github.com/mcgeer/WastedBankSpace, Hub commit `1cb3b28`),
  plus unmerged PRs: STASH units and tag tab by Brettgod1355 (#136, #146, branch `stash-units` @ `e372558`),
  data fixes #124, #125, #132, #133. All BSD-2. See docs/attribution.md.

## Behaviour
After the bank has been opened this session:
- **Bank marker:** each storable bank item is marked in a configurable style (item outline, box outline or corner triangle; colour, outline thickness, triangle size and corner are configurable). Hovering lists every enabled
  place it can go, with at most 3 STASH units plus "...and N more".
- **Sidebar panel:** ranks storage locations by how many bank slots they would free, most first. POH builds are
  orange. Click a location to list its items. This answers "which upgrade frees the most space". An "Edit storage
  locations & tiers" button opens the plugin settings (`core/SettingsOpener`).
- **`storable` tag tab:** a virtual bank tag (core `TagManager.registerTag`) showing only flagged items. The tab is
  added to the Bank Tags tab list and removed again when the option or feature is turned off. A user's own tab
  with the same name is never removed.
- **Uses saved snapshots** (default on, `core/snapshots`): before the bank is opened this session, works from the
  bank as last seen (panel says how old). POH storage **stacks** (confirmed in-game, e.g. Bruma torch in the magic
  wardrobe), so copies already stored never stop a suggestion; the tooltip shows "(N stored)" and the panel counts
  them.
- **POH tiers** (#14): fancy dress box, armour case, magic wardrobe, cape rack and treasure chest only get
  suggestions that fit the tier you've built. Tier = config dropdown, or **auto-detected** from the furniture object
  (or its empty build space = not built) when you're in your costume room, saved per account. Count-limited storages
  get the sets that free the most bank slots; a set already stored keeps its slot; cape rack limits only capes of
  accomplishment; treasure chest takes rewards up to its clue tier. Panel rows show the tier and "+N with <tier>"
  for the highest tier your real Construction level can build (also for storages that hold nothing yet); no hint
  if you can't build any higher tier. Recomputed when your Construction level changes. Unknown tier = no limit, "tier unknown: visit your costume room".
  Sets/pieces and their item IDs come from the OSRS Wiki via `tools/convert_wiki_storage_sets.py` → `storage-sets.json`;
  the same run generates `data/WikiStorageItems.java`, merged into the five costume room storages' item lists (#20).
- **Grouped storable tab** (default on): the `storable` tab gets a core layout with one group per container, a title
  and divider line above each (drawn by `StorableTitlesOverlay`), ordered most bank slots freed first. An item that
  fits several containers appears in each group. Turning it off removes only the layout we created.
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
Section "Bank Savings: General":
| Key | Type | Default |
|---|---|---|
| `storageSavingsEnabled` | boolean | true |
| `storageSavingsMarker` | boolean | true |
| `storageSavingsMarkerColor` | Color | (255,200,0) |
| `storageSavingsMarkerStyle` | Item outline / Box outline / Corner triangle | Corner triangle |
| `storageSavingsMarkerCorner` | Top/bottom left/right | Bottom right |
| `storageSavingsMarkerSize` | int 4–24 (triangle) | 8 |
| `storageSavingsMarkerThickness` | int 1–4 (outlines) | 1 |
| `storageSavingsTagTab` | boolean | true |
| `storageSavingsTagTabGroups` | boolean | true |
| `storageSavingsUseSnapshots` | boolean | true |

Section "Bank Savings: Tiers": `storageSavingsTier<Storage>` dropdowns (Auto-detect / Not built / tiers).
Detected tiers per RS profile: `storageSavingsTierDetected.<STORAGE>` (tier index, -1 = not built).
| `storageSavingsPanel` | boolean | true |
| `storageSavingsKeepBis` | boolean | true |
| `storageSavingsIgnored` | String | "" |
| `storageSavingsStash` | boolean | false |
| `storageSavingsStashBuiltOnly` | boolean | true |

Section "Bank Savings: Locations": one `storageSavings<Location>` toggle per `StorageLocation`, all on except
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
- [ ] Marker style: Item outline, Box outline, Corner triangle; colour, outline thickness, triangle size and corner
- [ ] Enter your costume room: tiers are detected (log "Detected FANCY_DRESS_BOX tier Oak" etc.); panel rows show
      the tier, e.g. "POH fancy dress box · Oak · +3 with Teak"
- [ ] Oak fancy dress box with 3+ costumes banked: only the 2 costumes with the most banked pieces are suggested
- [ ] Set a dropdown to a different tier: suggestions and "+N with …" update; back to Auto-detect restores
- [ ] Set one to "Not built": its row shows "not built · +N with Oak" and nothing is suggested for it
- [ ] Cape rack at oak: skillcapes not suggested, other capes are; treasure chest at oak: only beginner/easy rewards
- [ ] `storable` tab: items grouped under container titles with divider lines, biggest group first; scrolling keeps
      titles on their rows and inside the bank; an item fitting two containers shows in both groups
- [ ] Ignore an item / toggle a location: groups update while the tab is open
- [ ] "Group storable tab" off: plain ungrouped tab, no titles; on again: grouped
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

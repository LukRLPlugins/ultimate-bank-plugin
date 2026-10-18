# Layout tools

- **Package:** `com.ultimatebank.feature.layouttools`
- **Status:** see docs/roadmap.md
- **Sources:** Bank Tag Layouts by geheur (https://github.com/geheur/bank-tag-custom-layouts, Hub commit `d26417d`),
  BSD-2. Zigzag algorithm (`LayoutGenerator.zigzagLayout`) and row helpers (`Layout.insertBlankRow/removeBlankRow`).
  See docs/attribution.md.

## Behaviour
Builds on **core** bank tag layouts (Bank Tags plugin › right-click a tag tab › Enable layout). There's no
separate layout engine, so layouts stay compatible with core's import/export.

- **Zigzag auto-layout:** adds "Auto layout: Zigzag (Ultimate Bank)" to a laid-out tag tab's right-click menu
  (next to core's "Auto layout: Default"). It lays out worn gear, then inventory, each zigzagging down/up the
  columns of a pair of rows (16 items per pair), then rune pouch runes in a plain row. Items already in the layout
  stay put unless gear or inventory takes their spot; displaced items move to the first free spots afterwards.
  Core shows a "Keep / Undo" prompt.
  - Duplicate limit (default 4): repeated inventory items up to the limit get one slot each; longer runs collapse to
    one slot. 0 = no duplicates.
  - The name differs from Bank Tag Layouts' own "Zigzag" because core rejects duplicate auto layout names, and
    that plugin unregisters "Zigzag" by name on startup.
- **Insert empty row / Remove row:** shift-right-click empty space in a laid-out tab. Insert pushes that row and
  everything below down by one row. Remove is only offered on an empty row that isn't the last.

Dropped from the original (see roadmap): Presets style (same as core's "Default"), Add item, its own layout engine.

## Config
Section "Layout tools":
| Key | Type | Default |
|---|---|---|
| `layoutToolsEnabled` | boolean | true |
| `layoutToolsZigzag` | boolean | true |
| `layoutToolsZigzagDuplicateLimit` | int 0-28 | 4 |
| `layoutToolsZigzagRunePouch` | boolean | true |
| `layoutToolsRowMenus` | boolean | true |

## Implementation
- `LayoutMath`: pure functions on core-style `int[]` layouts (8 per row, -1 empty): `zigzag`, `limitDuplicates`,
  `insertRow`, `removeRow`, `isRowEmpty`, `rowCount`. Unit tested in `LayoutMathTest`.
- `LayoutToolsFeature`: registers the auto layout with core `LayoutManager.registerAutoLayout` (unregisters on
  shutdown or toggle off); reads `InventoryID.WORN`/`INV` (canonicalized) and rune pouch varbits
  `RUNE_POUCH_TYPE_1..4` via `EnumID.RUNEPOUCH_RUNE`; row menus on `MenuOpened` with Shift held, saving via
  `LayoutManager.saveLayout` and re-opening the tag with `BankTagsService.openBankTag`.
- Mouse row: `(mouseY - Bankmain.ITEMS canvasY + scrollY + Y_PADDING/2) / (BANK_ITEM_HEIGHT + BANK_ITEM_Y_PADDING)`.

## In-game test checklist
- [ ] Open a tag tab, right-click the tab › Enable layout (core)
- [ ] Right-click the tab again › "Auto layout: Zigzag (Ultimate Bank)": gear fills rows 1-2 in a zigzag,
      inventory starts at row 3, rune pouch runes on their own row; Keep/Undo prompt works
- [ ] With 4 prayer potions: 4 separate slots; with 20 sharks: 1 slot. Change the limit and re-run
- [ ] Items you'd placed before that aren't in gear/inventory are still in the tab afterwards
- [ ] Shift-right-click empty space between items › Insert empty row: everything from that row moves down
- [ ] Shift-right-click an empty row › Remove row: rows below move up. Not offered on rows with items
- [ ] Without Shift: no extra options. On items: no extra options
- [ ] Scrolled down in a long tab: rows are still inserted where you clicked
- [ ] Turn Zigzag off in config: the menu option disappears. Turn the feature off: both tools disappear
- [ ] With Bank Tag Layouts (hub) also installed: both "Zigzag" entries appear and work independently

## Open questions
- Lock layouts (#107), mirrored zigzag (#91), View tag tabs by default (#98): deferred by user.

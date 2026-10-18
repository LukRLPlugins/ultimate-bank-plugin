# Roadmap

Status: `idea` → `researching` → `in-progress` → `testing` (awaiting in-game confirmation) → `done`.
All sources below are BSD-2-Clause (checked 2026-10-18). Re-check the license before porting anything new.

| Feature | Status | Source plugin(s) | Repo |
|---|---|---|---|
| Tag layouts | idea | Bank Tag Layouts (geheur). Core RuneLite now includes layouts too | https://github.com/geheur/bank-tag-custom-layouts |
| Inventory setups | idea | Inventory Setups (dillydill123) | https://github.com/dillydill123/inventory-setups |
| Bank value | idea | Bank Value (spudjb) | https://github.com/spudjb/runelite-bank-value |
| Better search | idea | Better Bank Search (KaasPeer1), Extended Bank Search (khendricksen) | https://github.com/KaasPeer1/better-bank-search, https://github.com/khendricksen/runelite-extended-bank-search |
| Storage savings | done (v1) | Wasted Bank Space (mcgeer) + unmerged PRs (Brettgod1355 and others). Still to compare: POH Storage (tcpowell), Bank POH Search (Wozak-1) | https://github.com/mcgeer/WastedBankSpace, https://github.com/tcpowell/poh-storage, https://github.com/Wozak-1/bank-poh-searcher |
| Costume room highlighter | idea | Costume Room Highlighter (michaelperino) | https://github.com/michaelperino/CostumeRoomChecker |

## Feature notes

### Tag layouts
- Core bank tags already support layouts (`net.runelite.client.plugins.banktags.tabs.Layout`, `BankTagsService`). First decide which parts of geheur's plugin core doesn't cover yet (e.g. auto-layout presets, duplicate items, layout import/export).
- Desired tweaks: _TBD with user_

### Inventory setups
- Upstream filters the bank via `TagManager.registerTag` + `BankTagsService.openBankTag`.
- Desired tweaks: _TBD with user_

### Bank value
- Desired tweaks: _TBD with user_

### Better search
- Desired tweaks: _TBD with user_

### Storage savings
- Goal: show which POH storage upgrades (costume room: armour case, cape rack, toy box, treasure chest, magic wardrobe, etc.) would free up the most bank slots given what's currently banked.
- Design doc: docs/features/storage-savings.md
- Desired tweaks (decided 2026-10-18):
  - Keep all four displays: **upgrade ranking** (new), bank marker + tooltip, `storable` tag tab, sidebar item list
  - Add **STASH units** (from upstream PR #146)
  - Ignore list: keep **both** shift-click toggle and the names/IDs/wildcards text box
  - Location defaults match the original: all on except armour case, cape rack, pet house
  - Not wanted for now: remembering the bank across restarts, marking inventory/equipment items

### Costume room highlighter
- Highlight banked items that could be stored in the POH costume room.
- Likely shares item-to-storage data with "POH storage savings". Build one shared data set in `core/`.
- Desired tweaks: _TBD with user_

## Other ideas
- _Add here as they come up._

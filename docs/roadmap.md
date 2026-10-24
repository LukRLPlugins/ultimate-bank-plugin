# Roadmap

**Live status lives on the project board: https://github.com/orgs/LukRLPlugins/projects/1** (one issue per feature or
idea). This file keeps the decisions behind each feature: sources, licenses and the user's desired tweaks.

Status: `idea` → `researching` → `in-progress` → `testing` (awaiting in-game confirmation) → `done`.
All sources below are BSD-2-Clause (checked 2026-10-18). Re-check the license before porting anything new.

| Feature | Status | Source plugin(s) | Repo |
|---|---|---|---|
| Sorted tabs | parked | Bank Skill Sorting (Frailrain). Phase 1 built, parked on branch `sorted-tabs` | https://github.com/Frailrain/Bank-Skill-Sorting |
| Tag layouts (Layout tools) | won't do (#12, closed 2026-10-23) | Bank Tag Layouts (geheur). Built, works, parked on branch `layout-tools` | https://github.com/geheur/bank-tag-custom-layouts |
| Inventory setups | won't do (#9, closed 2026-10-23) | Inventory Setups (dillydill123) | https://github.com/dillydill123/inventory-setups |
| Bank value | folded into Ultimate Bank Search: total, quantity sort, GE/HA toggle, value share (#10, PR #39); in v1.3.0 | Bank Value (spudjb) | https://github.com/spudjb/runelite-bank-value |
| Better search | won't do (#11, closed 2026-10-23) | Better Bank Search (KaasPeer1), Extended Bank Search (khendricksen) | https://github.com/KaasPeer1/better-bank-search, https://github.com/khendricksen/runelite-extended-bank-search |
| Storage savings | released in v1.0.0 (Hub PR runelite/plugin-hub#18906); v1.1.0 released (runelite/plugin-hub#19211); storable tab order + focus (#32, PR #38) in v1.3.0 | Wasted Bank Space (mcgeer) + unmerged PRs (Brettgod1355 and others). Still to compare: POH Storage (tcpowell), Bank POH Search (Wozak-1) | https://github.com/mcgeer/WastedBankSpace, https://github.com/tcpowell/poh-storage, https://github.com/Wozak-1/bank-poh-searcher |
| Ultimate Bank Search (was Where's my stuff) | done (#21, PR #33); v1.5.1 submitted (runelite/plugin-hub#19323); off for existing installs. More storages snapshotted (#41: containers PR #42, leprechaun + fossils PR #43) in v1.4.0; key ring, scroll book, NMZ, Elnock, Hallowed tools, potion storage and live pet house (#41 part 3, PR #44) in v1.5.0; spice rack and Flamtaer bag not readable on the client | Where's My Stuff / Bank POH Search (Wozak-1) | https://github.com/Wozak-1/bank-poh-searcher |
| Bank Tweaks | done: View tag tabs on left-click (#28, PR #40); in v1.4.0 | original | |
| Costume room highlighter | won't do (#8, closed 2026-10-23) | Costume Room Highlighter (michaelperino) | https://github.com/michaelperino/CostumeRoomChecker |

## Feature notes

### Sorted tabs (parked)
- Auto-sorted skill tabs with titled sections, ported from Bank Skill Sorting (phase 1 of 4, plus `storable (ub)`).
  Worked in-game, but the user didn't like the result (2026-10-18); **no tag tab layouts for now**. Parked on branch
  `sorted-tabs` (design doc `docs/features/sorted-tabs.md` there, data converter `tools/convert_bss_tabs.py`).

### Tag layouts (parked)
- Built as "Layout tools" (zigzag auto-layout, insert/remove rows) on branch `layout-tools`; works in-game but wasn't
  what the user wanted. Not merged. Core already covers layouts, placeholders, duplicates, import/export.

### Inventory setups
- Upstream filters the bank via `TagManager.registerTag` + `BankTagsService.openBankTag`.
- Desired tweaks: _TBD with user_

### Bank value
- Decided 2026-10-22: no separate panel (Bank Value is a sortable item table, which Ultimate Bank Search already is).
  Add its missing bits and top requests to the Bank Search Items tab: a grand total, sort by quantity, GE / high alch
  toggle, each item's share. Core's bank title already shows the open tab's value.

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

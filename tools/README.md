# tools

One-off data converters used when porting from other plugins. Run them on a shallow clone of the source repo
(in a scratch directory, never inside this repo) and commit the output.

| Script | Produces | Source |
|---|---|---|
| `fetch_wiki_item_ids.py <out.json>` | wiki item name → item ID table (scratch, input to the next row) | OSRS Wiki `infobox_item` bucket API |
| `convert_wiki_storage_sets.py <raw wiki dir> <wiki-item-ids.json> <gameval.txt> <out.json> <out.java>` | `storage-sets.json` (sets + item IDs) and `data/WikiStorageItems.java` (generated) | OSRS Wiki "<storage> space" pages (`?action=raw`) |
| `convert_wbs_locations.py <conv dir> <locations dir> <out dir>` | `feature/storagesavings/data/*Items.java` (legacy ItemID to gameval) | Wasted Bank Space |

`convert_wbs_locations.py` needs a conv dir holding `legacy.txt` / `gameval.txt` (`javap -constants` dumps of
`net.runelite.api.ItemID` and `net.runelite.api.gameval.ItemID`, one `NAME = value` per line) and `header.txt`.
`header.txt` must use `Copyright (c) 2026, Luk` for our line (CLAUDE.md › Identity).

`make_icon/MakeIcon.java` draws the Plugin Hub `icon.png` (48x72): `java tools/make_icon/MakeIcon.java icon.png [preview.png]`.

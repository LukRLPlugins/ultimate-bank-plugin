# tools

One-off data converters used when porting from other plugins. Run them on a shallow clone of the source repo
(in a scratch directory, never inside this repo) and commit the output.

| Script | Produces | Source |
|---|---|---|
| `convert_wbs_locations.py <conv dir> <locations dir> <out dir>` | `feature/storagesavings/data/*Items.java` (legacy ItemID to gameval) | Wasted Bank Space |

`convert_wbs_locations.py` needs a conv dir holding `legacy.txt` / `gameval.txt` (`javap -constants` dumps of
`net.runelite.api.ItemID` and `net.runelite.api.gameval.ItemID`, one `NAME = value` per line) and `header.txt`.
`header.txt` must use `Copyright (c) 2026, Luk` for our line (CLAUDE.md › Identity).

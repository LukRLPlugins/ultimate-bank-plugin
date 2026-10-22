# Ultimate Bank

A bank toolkit for [RuneLite](https://runelite.net). It finds the items in your bank that could live somewhere else,
shows which storage upgrades would free up the most bank space, and lets you search everything you own across your
bank and POH storage.

Open your bank and every item that could live somewhere else gets a marker, while the sidebar panel ranks where
the space would come from:

<table>
  <tr>
    <td align="center" valign="top">
      <img src="docs/images/bank-markers.png" alt="Bank items with storable markers" height="420"><br>
      <sub>Green corner markers on items that could be stored outside the bank</sub>
    </td>
    <td align="center" valign="top">
      <img src="docs/images/storage-panel.png" alt="Ultimate Bank Savings panel ranking storage by bank slots freed" height="420"><br>
      <sub>The Ultimate Bank Savings panel: each storage, the bank slots it frees,<br>your POH tier and what the next upgrade adds</sub>
    </td>
  </tr>
</table>

## Ultimate Bank Savings
- **Storable markers:** every banked item that could be stored elsewhere is marked (corner triangle, item outline or
  box outline; colour, size and corner are up to you). Hover it to see
  where: POH costume room (treasure chest, fancy dress box, magic wardrobe, armour case, cape rack, toy box),
  bookcase, spice rack, pet house, tackle box, seed vault, tool leprechaun, huntsman's kit, forestry kit, steel key
  ring, master scroll book, fossil storage, Nightmare Zone, and more. Tooltips also say how many copies a POH storage
  already holds.
- **Upgrade ranking:** a sidebar panel (chest icon) ranks storage by how many bank slots it would free, and shows what
  the best upgrade your Construction level can build adds, e.g. "POH fancy dress box · Oak · +3 with Teak". Click a
  row to see its items, or "Edit storage locations & tiers" to jump to the settings.
- **Knows your POH tiers:** visit your costume room and it detects which furniture you've built, then only suggests
  what fits (an oak fancy dress box holds 2 costumes, so you get the 2 that free the most slots). You can also set
  each tier in the config.
- **`storable` bank tag tab:** every storable item in one tab, grouped by container with a title over each group.
- **Works before you open the bank:** your last-seen bank and POH storage contents are remembered per account.
- **STASH units** (optional): also flag items for STASH units you've built but not filled.
- **Ignore list:** shift-right-click a marked item › *Ignore storable*, or list names, IDs or `*` wildcards in config.
  Best-in-slot items (skillcapes, max cape, Ancestral, ...) are never flagged unless you turn that off.

The `storable` tab gathers everything in one place, grouped by where it can go, and the POH tiers are detected for
you (or set by hand):

<table>
  <tr>
    <td align="center" valign="top">
      <img src="docs/images/storable-tab.png" alt="The storable tag tab, grouped by container" height="420"><br>
      <sub>The <code>storable</code> bank tag tab, one titled group per storage</sub>
    </td>
    <td align="center" valign="top">
      <img src="docs/images/poh-tiers.png" alt="POH tier settings"><br>
      <sub>POH tier settings: auto-detected in your costume room,<br>or pick a tier per storage</sub>
    </td>
  </tr>
</table>

## Ultimate Bank Search
A sidebar panel (magnifier icon) that remembers your bank and each POH costume room storage, per account:
- **Items:** everything you own, grouped by location with item counts, total value and when each was last seen.
  Search by item or location name to find where something is.
- **History:** what each visit to the bank or a storage took out or put in, newest first, with the value change.

<table>
  <tr>
    <td align="center" valign="top">
      <img src="docs/images/search-items.png" alt="Ultimate Bank Search items tab: the bank grouped by location with quantities and values" height="420"><br>
      <sub>Items: everything in a location, most valuable first</sub>
    </td>
    <td align="center" valign="top">
      <img src="docs/images/search-history.png" alt="Ultimate Bank Search history tab: what each bank and POH visit changed" height="420"><br>
      <sub>History: what each visit took out or put in</sub>
    </td>
  </tr>
</table>

Open your bank and each POH storage once so they're recorded. On by default for new installs; if you're updating,
turn it on under **Configuration › Ultimate Bank › Bank Search: General**.

Find everything under **Configuration › Ultimate Bank**. Each part can be turned off on its own.

## Development
Requires JDK 11.

```sh
./gradlew build   # compile + tests
./gradlew run     # launch RuneLite with the plugin loaded
```

To log in to the dev client with a Jagex account, see
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Credits
Built on ideas and code from other BSD-licensed RuneLite plugins (Wasted Bank Space, Where's My Stuff, Bank Skill
Sorting) and storage data from the Old School RuneScape Wiki. See [docs/attribution.md](docs/attribution.md).

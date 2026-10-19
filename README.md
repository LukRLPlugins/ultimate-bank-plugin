# Ultimate Bank

A bank toolkit for [RuneLite](https://runelite.net). It finds the items in your bank that could live somewhere else,
and shows which storage upgrades would free up the most bank space.

<!-- Screenshot: bank with storable markers + the Storage savings panel -->

## Storage savings
- **Storable markers:** a small corner marker on every banked item that could be stored elsewhere. Hover it to see
  where: POH costume room (treasure chest, fancy dress box, magic wardrobe, armour case, cape rack, toy box),
  bookcase, spice rack, pet house, tackle box, seed vault, tool leprechaun, huntsman's kit, forestry kit, steel key
  ring, master scroll book, fossil storage, Nightmare Zone, and more. Tooltips also say how many copies a POH storage
  already holds.
- **Upgrade ranking:** a sidebar panel (chest icon) ranks storage by how many bank slots it would free, and shows what
  the next upgrade adds, e.g. "POH fancy dress box · Oak · +3 with Teak". Click a row to see its items.
- **Knows your POH tiers:** visit your costume room and it detects which furniture you've built, then only suggests
  what fits (an oak fancy dress box holds 2 costumes, so you get the 2 that free the most slots). You can also set
  each tier in the config.
- **`storable` bank tag tab:** every storable item in one tab, grouped by container with a title over each group.
- **Works before you open the bank:** your last-seen bank and POH storage contents are remembered per account.
- **STASH units** (optional): also flag items for STASH units you've built but not filled.
- **Ignore list:** shift-right-click a marked item › *Ignore storable*, or list names, IDs or `*` wildcards in config.
  Best-in-slot items (skillcapes, max cape, Ancestral, ...) are never flagged unless you turn that off.

<!-- Screenshot: the grouped storable tab -->

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

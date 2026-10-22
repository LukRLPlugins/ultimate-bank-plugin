# RuneLite API notes (bank-related)

Living document. Add anything non-obvious you discover, with the RuneLite version it applied to.
Current RuneLite client version at setup: **1.13.2**.

## Looking things up
- Signatures: `javap -cp "$(find ~/.gradle/caches -name 'client-*.jar' | grep -v sources | head -1)" <fully.qualified.Class>`
- Source: https://github.com/runelite/runelite (`runelite-client/src/main/java/net/runelite/client/plugins/bank*`)
- IDs: always use `net.runelite.api.gameval.*` (`ItemID`, `InterfaceID`, `InventoryID`, `VarbitID`), never raw numbers.

## Bank contents
- `ItemContainerChanged` with `event.getContainerId() == InventoryID.BANK` fires when the bank changes. `client.getItemContainer(InventoryID.BANK)` returns null when the bank has never been opened this session.
- Placeholders show up with quantity 0. Filter them when counting items or slots.
- Noted / placeholder IDs: canonicalise with `ItemManager.canonicalize(id)` before price lookups.

## Bank UI
- The bank is rebuilt by the client script `BANKMAIN_BUILD`. Hook `ScriptPostFired` (or `ScriptCallbackEvent`) for redraw-safe widget changes. Widgets you modify get reset on every rebuild.
- Get widgets via `client.getWidget(InterfaceID.Bankmain.<COMPONENT>)`.

## Core bank tags (reuse instead of reimplementing)
- `BankTagsService`: `openBankTag(String tag, int options)` with `OPTION_ALLOW_MODIFICATIONS | OPTION_HIDE_TAG_NAME | OPTION_NO_LAYOUT | OPTION_ITEMS_NOT_IN_LAYOUT_AT_BOTTOM`, `closeBankTag()`, `getActiveTag()`, `getActiveBankTag()`, `getActiveLayout()`.
- `TagManager`: `registerTag(String, BankTag)` / `unregisterTag` for predicate-based virtual tags (`BankTag.contains(int itemId)`), plus `getItemsForTag`, `addTag`, `removeTag`, `renameTag`, `isHidden`/`setHidden`.
- `tabs.Layout`, `tabs.LayoutManager`, and `tabs.AutoLayout` hold the layout model.

## Gotchas
- **`BankTagsService` / `TagManager` need `@PluginDependency(BankTagsPlugin.class)`** on the plugin (already on
  `UltimateBankPlugin`). `BankTagsService` is implemented by `BankTagsPlugin` itself, so it's only injectable from a
  dependent plugin's child injector.
- A `TagManager.registerTag` tag only shows as a tab if its name is in the Bank Tags config list
  (`BankTagsPlugin.CONFIG_GROUP` / `TAG_TABS_CONFIG`, CSV), with an icon under `TAG_ICON_PREFIX + name`.
  Only remove tabs you added (track it with your own config flag).
- Bank placeholders: `itemManager.getItemComposition(id).getPlaceholderTemplateId() != -1`.
- Bank item widgets have parent `InterfaceID.Bankmain.ITEMS`. Checking this keeps overlays and menus off tag tab icons.
- `ConfigChanged` can arrive on the Swing thread (user edits in the config panel). Hop to the client thread
  before touching client state.
- Legacy `net.runelite.api.ItemID` names differ from `gameval.ItemID` names. Convert by numeric value
  (`javap -constants` on both classes); never by name.
- `client.runScript(ScriptID.WATSON_STASH_UNIT_CHECK, objectId, s1, s2, s3)` returns (built, filled) on the int stack.
  See `StashTracker` for where s1..s3 come from.
- Menus: use `client.getMenu().createMenuEntry(i)` (not the deprecated `client.createMenuEntry`) with `MenuAction.RUNELITE`.

- **Virtual tag + layout:** a `TagManager.registerTag` tag can carry a core layout: `layoutManager.saveLayout(new Layout(tag))`
  then `bankTagsService.openBankTag(tag, OPTION_ALLOW_MODIFICATIONS)` re-reads it. Items matching the tag but missing
  from the layout get auto-placed into gaps, so the layout should position every owned member.
- The bank container fires `ItemContainerChanged` on every open even when unchanged. Fingerprint ids+quantities and
  skip rebuilds when equal, or opening the bank hitches (Bank Skill Sorting issues #5/#12).
- `ItemManager.canonicalize` turns noted items and placeholders into the real item; `ItemVariationMapping.map` gives
  the variation base (degraded/charged/ornament variants).

- **POH costume room storage** (`InterfaceID.POH_COSTUMES`, side panel `POH_COSTUMES_SIDE`): items fill in ~2 ticks
  after `WidgetLoaded`. Stored items are the widgets under `PohCostumes.ITEMS` with opacity 0 (unstored ones are
  faded). Which storage it is only shows in the interface text ("Toy box", "Magic wardrobe", ...). `PohCostumes.TIER`
  text is **empty** (checked in-game for fancy dress box, treasure chest, magic wardrobe), so it can't give the tier.
  Taking out or putting in an item fires no event of its own; key a rescan off the inventory `ItemContainerChanged`
  (next tick) while `PohCostumes.UNIVERSE` exists.
- **POH storage stacks**: almost every item can be stored more than once (e.g. Bruma torches in the magic wardrobe).
  Assume stacking unless a storage is known not to; never treat "already stored" as "can't store".
- **Costume room furniture tiers** are separate objects: `ObjectID.POH_COS_ROOM_<STORAGE>_<TIER>` (+ `_OPEN_` variants;
  treasure chest is spelled `TRESURE`), and `POH_COS_ROOM_<STORAGE>_HOTSPOT` is the empty build space (only in
  building mode). Detect on `GameObjectSpawned`.
- Bank placeholders have quantity 0 in the bank `ItemContainer`.

- **Opening our settings from a panel:** there's no public API (`TopLevelConfigPanel` is package-private). The core
  Configuration plugin opens a plugin's settings on `OverlayMenuClicked` with `MenuAction.RUNELITE_OVERLAY_CONFIG`
  and an overlay whose `getPlugin()` is that plugin, so `core/SettingsOpener` posts one for a never-added overlay.
  It can't jump to or expand a section.

- **Config defaults aren't saved:** `ConfigManager.setDefaultConfiguration` unsets values equal to the default, so a
  `@ConfigItem` default can't tell new installs from updates. `getConfigurationKeys("<group>.")` lists the saved keys
  of the current RuneLite profile **only**: RS-profile (per-account) data lives in a separate store shared by all
  RuneLite profiles (`profiles2/$rsprofile--1.properties`). Check it with `getRSProfiles()` +
  `getRSProfileConfigurationKeys(group, profile.getKey(), "")` (`core/FeatureRollout` checks both).

- **Settings are per RuneLite profile, not per character.** Several characters often share one RuneLite profile. For
  data that belongs to a character use `set/getRSProfileConfiguration` (keyed by account). For a `@ConfigItem` that
  should differ per character, save a copy per RS profile and load it into the setting on `RuneScapeProfileChanged`
  (see `storagesavings/CharacterSettings`). `ConfigManager` posts `ConfigChanged` synchronously, so a flag set around
  your own writes tells them apart from the user's; RS-profile writes have `event.getProfile() != null`.

## Tooling gotchas
- The compile hook runs after **every** edit, so when an edit needs new imports, add the imports first or in the
  same edit. Otherwise you get a spurious "cannot find symbol" in between.
- javap on Windows needs a `;`-separated classpath of Windows paths: `cygpath -w` each jar.

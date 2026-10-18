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

## Tooling gotchas
- The compile hook runs after **every** edit, so when an edit needs new imports, add the imports first or in the
  same edit. Otherwise you get a spurious "cannot find symbol" in between.
- javap on Windows needs a `;`-separated classpath of Windows paths: `cygpath -w` each jar.

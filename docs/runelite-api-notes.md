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
- _None recorded yet._

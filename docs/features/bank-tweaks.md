# Bank Tweaks

- **Package:** `com.ultimatebank.feature.banktweaks`
- **Status:** see docs/roadmap.md
- **Sources:** original (#28). #27 (custom left-click bank actions) would also go here.

## Behaviour
Small changes to what clicks in the bank do. Each one only **reorders a menu's existing options**, like core's Menu
Entry Swapper: no new menu entries, nothing new sent to the server (Hub menu rules).

- **Left-click: view tag tabs (#28):** core Bank Tags' "+" button below the tag tabs left-clicks to **View tag tabs**
  (the list of every tag tab) instead of **New tag tab**. Right-click still has New / Import tag tab. Only applies
  while core Bank Tags shows the button.

Ships **on for new installs, off for updates** (`bankTweaksEnabled` is in `NEW_FEATURE_TOGGLES`).

## Config
Section "Bank Tweaks" (closed by default).

| Key | Type | Default | Description |
|---|---|---|---|
| `bankTweaksEnabled` | boolean | true (switched off once on existing installs) | Turns the tweaks below on |
| `bankTweaksViewTagTabs` | boolean | true | "+" button left-click lists tag tabs |

## Implementation
- Events subscribed: `PostMenuSort` (skipped while a menu is open). If the left-click entry is "New tag tab", it's
  swapped with the same widget's "View tag tabs" entry. Core's `TabInterface` handles the click by its op.
- Pure logic: `BankTweaksFeature.leftClickSwap` (tested in `BankTweaksFeatureTest`).
- Nothing persisted, nothing drawn; `shutDown()` has nothing to undo.

## In-game test checklist
- [ ] Existing install (dev profile): "Bank Tweaks › Enable" starts **off**. Tick it.
- [ ] Bank open: hover the "+" button below the tag tabs: the top-left text says "View tag tabs"; left-click lists
      your tag tabs
- [ ] Right-click "+": New tag tab and Import tag tab are still there and work
- [ ] Untick "Left-click: view tag tabs" (or Enable): left-click makes a new tab again
- [ ] Other bank buttons and items keep their normal left-click

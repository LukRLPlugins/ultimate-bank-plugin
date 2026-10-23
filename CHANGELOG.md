# Changelog

What changed in each version of Ultimate Bank, newest first.

## 1.3.0
**New**
- **Bank value** in Ultimate Bank Search: a total of everything you own (or of what your search matches) at the top
  of the Items tab, each item's share of it on hover, and sort by quantity.
- Value items at their **GE price** or **high alch** value (switch in the panel, or *Value items at* in the settings).

**Changed**
- `storable` tab: groups keep their order while the bank is open, so the storage you're emptying doesn't jump down
  the list. Click a group's title to show only that storage, and click it again to show all.

## 1.2.3
**Fixed**
- New features now really start **off** when you update and **on** for new installs (Ultimate Bank Search was
  starting on for everyone).

## 1.2.2
**Fixed**
- Per-character settings (POH tiers, storage locations, STASH options) are no longer replaced when RuneLite switches
  profile, by hand or through an account's default profile at login.

## 1.2.1
- Clearer README, this changelog, and a shorter Plugin Hub description. No changes in game.

## 1.2.0
**New**
- **Ultimate Bank Search**, a new sidebar panel (magnifier icon):
  - **Items:** everything in your bank and POH costume room storage, grouped by location, with counts, values and
    when you last saw each one. Search by item or location.
  - **History:** what each bank or storage visit took out or put in, with the value change.
- **Per-character settings:** POH tiers, storage locations and STASH options are remembered for each character, so
  characters sharing one RuneLite profile no longer overwrite each other's choices.

**Changed**
- Storage savings is now called **Ultimate Bank Savings**. The settings are grouped as *Bank Savings: General*,
  *Locations*, *Tiers* and *Bank Search: General*.
- New features now start **off** when you update (turn them on in the settings) and **on** for new installs.
  Ultimate Bank Search is the first.

## 1.1.0
**New**
- An **Edit storage locations & tiers** button in the Bank Savings panel opens the settings.

**Changed**
- Upgrade hints only suggest POH furniture your Construction level can build.

**Fixed**
- The "(N stored)" tooltip no longer counts items you've just taken out of POH storage.
- Kittens are no longer suggested for the pet house (only grown cats can be stored).
- Each bank visit is saved once, instead of every time an item moves.

## 1.0.0
First release: **Storage savings**.
- Marks banked items that could be stored somewhere else, and says where.
- A sidebar panel ranks storage by how many bank slots it would free, and what each POH upgrade adds.
- Detects your POH costume room tiers, so it only suggests what fits.
- A `storable` bank tag tab with every storable item, grouped by where it goes.
- Optional STASH units, an ignore list, and works from your last-seen bank before you open it.

---
name: new-feature
description: Scaffold a new Ultimate Bank feature module (package, BankFeature class, config section + toggle, design doc, roadmap entry). Use when starting any new bank feature.
argument-hint: <feature name, e.g. "bank value">
---

# New feature: $ARGUMENTS

1. **Clarify first.** Read `docs/roadmap.md` for this feature's sources and desired tweaks. If behaviour is unclear, ask the user what they want it to do differently from the source plugin(s) before writing code. If it's ported from a plugin, follow `/port-from-plugin` for the research step.
2. **Design doc.** Copy `docs/features/_template.md` to `docs/features/<name>.md` and fill in behaviour, config keys, events, and the test checklist. Keep it short.
3. **Package.** Create `src/main/java/com/ultimatebank/feature/<name>/` with `<Name>Feature.java`:
   ```java
   @Singleton
   public class <Name>Feature implements BankFeature
   {
   	@Override
   	public boolean isEnabled(UltimateBankConfig config)
   	{
   		return config.<name>Enabled();
   	}

   	@Override
   	public void startUp() { /* add overlays, build state */ }

   	@Override
   	public void shutDown() { /* remove everything startUp added */ }

   	// @Subscribe methods work directly. The feature is on the EventBus only while enabled.
   }
   ```
   Put overlays and helpers in the same package. Only move code to `core/` once a second feature needs it.
4. **Config.** In `UltimateBankConfig`, add a `@ConfigSection` (`closedByDefault = true`, next `position`) and a `<name>Enabled` toggle as the section's first item, `default true`. Prefix all keys with the feature name.
5. **Register.** Add `<Name>Feature.class` to `FEATURES` **and** its `"<name>Enabled"` key to `NEW_FEATURE_TOGGLES` in `UltimateBankPlugin`. New features ship on for new installs and off for people updating (`core/FeatureRollout`, the user's rule). Never remove a key from that list.
6. **Roadmap.** Set the status to `in-progress` in `docs/roadmap.md`.
7. **Build.** `./gradlew build` must pass.
8. **Hand off.** Offer `./gradlew run`, give the user the test checklist from the design doc, set the status to `testing`, and wait for confirmation before marking it `done`.

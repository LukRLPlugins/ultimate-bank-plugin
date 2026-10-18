# Plugin Hub submission checklist

Run `/hub-check` to audit these automatically before each release.

## Repo
- [ ] Public GitHub repo, `LICENSE` is BSD-2-Clause
- [ ] `runelite-plugin.properties`: displayName, author, description, tags, `plugins=com.ultimatebank.UltimateBankPlugin`
- [ ] Optional `icon.png` at repo root (max 48x72, optimised PNG)
- [ ] `build.gradle` still matches the example-plugin structure, Java 11 release
- [ ] No `com.example` / template leftovers, no build artifacts, no `META-INF/services` plugin file
- [ ] `README.md` describes features with screenshots

## Code
- [ ] Nothing from the forbidden list in AGENTS.md (reflection, JNI, processes, dynamic code, serialization, input injection, ...)
- [ ] No network calls, or any that exist are opt-in with the third-party warning
- [ ] Everything added in `startUp`/feature `startUp` is removed in `shutDown`
- [ ] No config key or group renames without a migration
- [ ] `docs/attribution.md` is complete. Copyright headers are kept on substantially copied code

## Submitting
1. Fork https://github.com/runelite/plugin-hub
2. Add `plugins/ultimate-bank` with:
   ```
   repository=https://github.com/<user>/ultimate-bank-plugin.git
   commit=<full 40-char commit hash>
   ```
3. Open a PR. For updates, change only `commit=`. Append `Generated-by: Claude Code` to the PR description (AGENTS.md › Submitting).

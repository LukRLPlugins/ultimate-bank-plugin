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
   repository=https://github.com/LukRLPlugins/ultimate-bank-plugin.git
   commit=<full 40-char commit hash>
   authors=Luk-Plugins
   ```
   `authors=` is required because the plugin repo is owned by an org: it names the GitHub account(s) allowed to
   submit updates (a reviewer asked for it on PR #18906).
3. Open a PR. For updates, change only `commit=`. Append `Generated-by: Claude Code` to the PR description (AGENTS.md › Submitting).

## Releases
| Version | Commit | Hub PR | Status |
|---|---|---|---|
| v1.0.0 | `d238b96` | https://github.com/runelite/plugin-hub/pull/18906 | submitted 2026-10-18 |

**Fork:** use one in the **LukRLPlugins org** (`gh repo fork runelite/plugin-hub --org LukRLPlugins`), so everything
for these plugins lives in the org. v1.0.0 was submitted from a user-account fork (`Luk-Plugins/plugin-hub`) by
mistake; delete that fork once PR #18906 is merged (its branch must stay until then). Local clone: `../plugin-hub`.
PRs are always opened by a user (Luk-Plugins), so expect RuneLite's bot to label them `non-author plugin change`
(org-owned plugin repo vs user author); it's informational.

**To release an update:** tag `vX.Y.Z` on `main`, run `/hub-check` (config keys diffed against the previous tag), then
on the org fork change only `commit=` in `plugins/ultimate-bank` and open a PR (or push to the open one).

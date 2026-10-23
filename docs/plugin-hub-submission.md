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
| v1.0.0 | `d238b96` | https://github.com/runelite/plugin-hub/pull/18906 | merged 2026-10-19 |
| v1.1.0 | `bfb48a1` | https://github.com/runelite/plugin-hub/pull/19211 | merged 2026-10-22 |
| v1.2.0 | `28e9591` | https://github.com/runelite/plugin-hub/pull/19219 | superseded by v1.2.1 in the same PR (never merged on its own) |
| v1.2.1 | `c7e4c8e` | https://github.com/runelite/plugin-hub/pull/19219 | superseded by v1.2.2 in the same PR (never merged on its own) |
| v1.2.2 | `baa7f97` | https://github.com/runelite/plugin-hub/pull/19219 | superseded by v1.2.3 in the same PR (never merged on its own) |
| v1.2.3 | `3670888` | https://github.com/runelite/plugin-hub/pull/19219 | superseded by v1.3.0 in the same PR (never merged on its own) |
| v1.3.0 | `aa1be17` | https://github.com/runelite/plugin-hub/pull/19219 | superseded by v1.4.0 in the same PR (never merged on its own) |
| v1.4.0 | `a3f3b1f` | https://github.com/runelite/plugin-hub/pull/19219 | submitted 2026-10-22 |

**Fork:** `LukRLPlugins/plugin-hub` (org fork, set up for v1.1.0), so everything for these plugins lives in the org.
Local clone: `../plugin-hub` (`origin` = org fork, `upstream` = runelite/plugin-hub). v1.0.0 went out from a
user-account fork (`Luk-Plugins/plugin-hub`) by mistake.
PRs are always opened by a user (Luk-Plugins), so expect RuneLite's bot to label them `non-author plugin change`
(org-owned plugin repo vs user author); it's informational.

**To release an update:**
1. Add the version to `CHANGELOG.md` (player-facing, short bullets under New / Changed / Fixed) and update the README's
   "New in" line if it's a minor or major version.
2. Run `/hub-check` (config keys diffed against the previous tag), then tag `vX.Y.Z` on `main`.
3. Create the GitHub Release from the changelog section:
   `gh release create vX.Y.Z -R LukRLPlugins/ultimate-bank-plugin --title "vX.Y.Z" --notes "<section>"`.
4. On the org fork change only `commit=` in `plugins/ultimate-bank` and open a PR (or push to the open one).

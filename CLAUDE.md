@AGENTS.md

# Ultimate Bank — project guide

An all-in-one RuneLite bank plugin that combines and tunes features from existing Plugin Hub
plugins (tag layouts, inventory setups, bank value, search, POH storage hints). **Target: Plugin
Hub from day one.** Every change must pass the rules in AGENTS.md (RuneLite's official agent
rules, kept verbatim from the example-plugin template; don't edit it, re-sync it from upstream).

## Identity (public)
- This project is published under the pseudonym **Luk** (GitHub org **LukRLPlugins**). Never write the maintainer's
  real name or personal email anywhere: code, copyright headers, docs, commit messages, issues, PRs.
- Copyright lines for our code: `Copyright (c) 2026, Luk`. Commits use the repo-local git identity (already set).
- Identity is set **by folder**: every repo under the local `LukRLPlugins` folder gets the Luk git identity and SSH
  key (global `includeIf`), and gh uses a Luk-only login (`GH_CONFIG_DIR`, set in `.claude/settings.local.json`
  and the shell profiles). Outside that folder only the personal identity exists, so the two can't cross.
- Before any `git push` or `gh` command that writes (issues, PRs, project, repo), run
  `bash .claude/scripts/board.sh whoami`. If it isn't Luk-Plugins, **stop and tell the user**. Never "fix" it by
  logging in or switching accounts yourself.
- Never commit machine paths (home folder, Windows username); machine-specific settings go in gitignored
  `.claude/settings.local.json`.

## Commands
- Build + tests: `./gradlew build`
- Fast compile check: `./gradlew compileJava compileTestJava -q` (a PostToolUse hook runs this after every .java edit, so fix reported errors immediately)
- Launch dev client: `./gradlew run`. Only the user can test in-game (see "Testing" in AGENTS.md); never drive the game client yourself.
- JDK: Temurin 11 at `C:\Program Files\Eclipse Adoptium\jdk-11*`. If `java` isn't found, set `JAVA_HOME` to it.

## Architecture
- `UltimateBankPlugin` is a thin shell. It holds the `FEATURES` list and forwards lifecycle and config changes to `core/FeatureManager`.
- `core/`: shared infrastructure only (the `BankFeature` contract, feature manager, and later the shared bank-state cache, pricing, and widget helpers). Features depend on core, never on each other.
- `feature/<name>/`: one package per feature. The entry class implements `BankFeature`, is enabled by its own toggle, gets registered on the EventBus only while enabled, and must fully undo itself in `shutDown()`.
- `UltimateBankConfig`: a single config group `ultimatebank`, with one `@ConfigSection` per feature. Key names are prefixed with the feature name (e.g. `bankValueShowHa`). Keys are permanent once released.
- Persisting non-config data (saved setups, layouts): use `ConfigManager` with per-profile keys, or `Filepath` per AGENTS.md. Never write raw files.
- To add a feature, follow the `/new-feature` skill.

## Building on core RuneLite, not around it
Core already ships bank tags with layouts. Before reimplementing anything, check whether core exposes it:
- `BankTagsService.openBankTag(tag, options)`, `getActiveLayout()`: open virtual tag tabs with layouts (this is how Inventory Setups filters the bank).
- `TagManager.registerTag(name, BankTag)`: virtual tags backed by a predicate.
- `ItemManager.getItemPrice / getItemComposition`, `ItemContainerChanged` for `InventoryID.BANK`.
- To inspect any RuneLite API signature: `javap -cp <client jar from ~/.gradle/caches> <class>`, or read https://github.com/runelite/runelite.
Running notes and gotchas live in `docs/runelite-api-notes.md`. Add to it whenever you learn something non-obvious.

## Porting from other plugins
All current source plugins are BSD-2-Clause (see `docs/roadmap.md`). When porting:
1. Read their implementation (shallow clone into the scratchpad, never into this repo).
2. Reimplement in our feature pattern. Don't paste whole files, and keep their copyright header on any substantially copied file.
3. Record the source, author, and license in `docs/attribution.md`.
Use the `/port-from-plugin` skill.

## Workflow (ideas → prototypes → approval)
- Board: https://github.com/orgs/LukRLPlugins/projects/1. Status: Ideas → Planned → In progress → Prototype ready → Done.
- `/idea` captures, `/prototype` builds a draft PR on `proto/<issue>-<slug>`, `/approve` merges after the user tests
  in-game (or applies feedback), `/status` summarizes. `.claude/scripts/board.sh` moves cards.
- Work on `main` only for small fixes or when the user asks; features go through a prototype PR.

## Docs map (read on demand)
- `docs/roadmap.md`: feature decisions: sources, licenses, the user's desired tweaks. Status lives on the board.
- `docs/runelite-api-notes.md`: bank widgets, events, and gotchas.
- `docs/features/<name>.md`: design notes per feature (behaviour, config keys, events used, open questions).
- `docs/plugin-hub-submission.md`: release checklist (also the `/hub-check` skill).
- `docs/attribution.md`: credits for ported code.

## Working style
- Small, focused commits, one feature or fix each. No reformatting mixed with logic (Hub reviewers read the diffs).
- Match the existing code style: tabs, Allman braces (RuneLite style), Lombok `@Slf4j`.
- When a feature is done, give the user a concrete in-game test checklist and wait for their confirmation (AGENTS.md › Testing).

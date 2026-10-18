---
name: port-from-plugin
description: Research an existing RuneLite Plugin Hub plugin and port (reimplement) its functionality into Ultimate Bank with proper license checks and attribution. Use when the user wants a feature "like plugin X".
argument-hint: <plugin-hub name or GitHub repo URL> [feature to port]
---

# Port from plugin: $ARGUMENTS

1. **Locate the source.** If you were given a Hub name, find its repo in `https://raw.githubusercontent.com/runelite/plugin-hub/master/plugins/<name>` (the `repository=` and `commit=` lines). The Hub page is `https://runelite.net/plugin-hub/show/<name>`.
2. **License gate.** Check the repo's LICENSE. BSD-2, BSD-3, MIT, and similar permissive licenses are OK. **GPL, no license, or "all rights reserved" means stop and tell the user.** We can only reimplement the idea from scratch, without reading the code closely.
3. **Clone outside the repo.** Shallow clone at the Hub's pinned commit into the session scratchpad, never into this project:
   `git clone --depth 1 <repo> <scratchpad>/<name>`
4. **Study it.** Summarise for the user: the features it offers, events and widgets it uses, persisted data/config keys, and known rough edges (check its open GitHub issues). Ask which parts to keep, drop, or change. Record the answers as "Desired tweaks" in `docs/roadmap.md`.
5. **Check core first.** See if RuneLite core already provides part of it (CLAUDE.md › "Building on core RuneLite"). Prefer core APIs over copied code.
6. **Reimplement** via `/new-feature`, following our patterns (BankFeature, prefixed config keys, ConfigManager persistence). If a file is substantially copied, keep its original copyright header and add ours.
7. **Attribute.** Add a row to `docs/attribution.md` and fill in "Sources" in the feature doc.
8. **Gotchas.** Copy any non-obvious API knowledge you learned into `docs/runelite-api-notes.md`.

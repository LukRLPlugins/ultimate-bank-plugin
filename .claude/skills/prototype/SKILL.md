---
name: prototype
description: Turn an idea issue into a working prototype on a branch with a draft PR for the user to review in-game. Use for "prototype #12", "build the top 3 ideas", or "try out <idea>".
argument-hint: <issue number(s), or "top N">
---

# Prototype: $ARGUMENTS

Goal: something the user can try in-game and approve or redirect. Keep it small; polish comes after approval.

1. `bash .claude/scripts/board.sh whoami`. Stop if it fails. Start from an up-to-date `main` (`git checkout main && git pull`).
2. Pick the issue(s). For "top N", use `bash .claude/scripts/board.sh list`, then take Planned first, then Ideas,
   oldest first. Confirm the pick with the user if there's more than one.
3. Per issue:
   - Read it plus all comments. If a design choice really changes what gets built, ask (offer options with
     descriptions). Otherwise pick the simplest reasonable option and note it in the PR.
   - `bash .claude/scripts/board.sh set <issue url> "In progress"`.
   - `git checkout -b proto/<issue#>-<slug>`.
   - Build it with the project's skills: `/new-feature` for a new feature, `/port-from-plugin` when it copies a Hub
     plugin, or plain edits for a change. Follow CLAUDE.md (feature toggles, config key prefixes, attribution).
   - `./gradlew build` must pass. Add unit tests for pure logic.
   - Commit, then `git push -u origin <branch>`.
   - `gh pr create -R LukRLPlugins/ultimate-bank-plugin --draft --title "<title>" --body` with: a summary, any
     decisions made for the user, "Closes #<issue>", and the **in-game test checklist**.
   - `bash .claude/scripts/board.sh set <PR url> "Prototype ready" "Ultimate Bank"` and the same for the issue.
     Add the `prototype` label to the issue.
4. Offer `/run-client` on that branch, and tell the user what to try. Never test in-game yourself (AGENTS.md › Testing).

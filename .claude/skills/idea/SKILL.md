---
name: idea
description: Capture a plugin idea as a GitHub issue on the LukRLPlugins board (Ideas column). Use when the user jots down an idea, feature request or "what if" for any of their plugins.
argument-hint: <the idea, as rough as you like>
---

# Capture an idea: $ARGUMENTS

Ideas are cheap: record it quickly, don't design it yet.

1. `bash .claude/scripts/board.sh whoami`. Stop if it fails (CLAUDE.md › Identity).
2. Search for duplicates: `gh issue list -R LukRLPlugins/ultimate-bank-plugin --state all --search "<keywords>"`.
   If one exists, add the new detail as a comment there instead and tell the user.
3. Only if the idea is too vague to act on later, ask **one** short question. Otherwise record it as-is.
4. Create it:
   `gh issue create -R LukRLPlugins/ultimate-bank-plugin --title "<short imperative title>" --label idea[,feature:<name>] --body "<What / Why / Inspired by>"`.
   Put the user's own words in the body. Add a `feature:<name>` label when it clearly belongs to one.
5. `bash .claude/scripts/board.sh set <issue url> Ideas "Ultimate Bank"`.
6. Reply with one line: the issue link.

Never put the user's real name or personal email in the issue (CLAUDE.md › Identity).

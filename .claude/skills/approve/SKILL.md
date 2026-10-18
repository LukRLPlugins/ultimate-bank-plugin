---
name: approve
description: Merge a prototype the user has tested and approved, or apply their feedback to it. Use when the user says a prototype works, "approve #N", "ship it", or gives changes for a draft PR.
argument-hint: <PR or issue number> [feedback]
---

# Approve or revise: $ARGUMENTS

1. `bash .claude/scripts/board.sh whoami`. Stop if it fails.
2. Find the draft PR (from the issue's linked PR if given an issue number):
   `gh pr view <n> -R LukRLPlugins/ultimate-bank-plugin --json number,title,headRefName,body,url`.
3. **Feedback instead of approval?** Check out the branch, make the changes, build, then commit and push. Comment on
   the PR with what changed. Keep the card in "Prototype ready" and ask the user to re-test. Stop here.
4. **Approval:** the user must have tested it in-game (if they haven't said so, ask; a passing build is not enough).
   - `gh pr ready <n>`, then `gh pr merge <n> --squash --delete-branch` (the squash message ends with the attribution lines).
   - `bash .claude/scripts/board.sh set <PR url> Done` and the same for the issue (merging closes it via "Closes #").
   - Update `docs/roadmap.md` / the feature doc status if a feature changed state. Commit and push to main.
   - `git checkout main && git pull`.
5. Reply with what was merged and the next items in Planned or Ideas.

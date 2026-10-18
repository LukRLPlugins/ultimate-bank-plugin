---
name: status
description: Summarize the LukRLPlugins board - what's waiting for the user's review, in progress, planned and the idea backlog. Use for "status", "what's next", "where are we", or at the start of a session.
---

# Board status

1. `bash .claude/scripts/board.sh list` (read-only; works under either account).
2. Also: `gh pr list -R LukRLPlugins/ultimate-bank-plugin --state open --json number,title,isDraft,updatedAt,url`.
3. Reply grouped in this order, one line per item (`#num title`, link):
   - **Waiting for your review** (Prototype ready): flag any not updated in 7+ days.
   - **In progress**
   - **Planned**
   - **Ideas** (count, plus the 5 oldest)
   - Recently done (last 5)
4. End with one suggested next step (e.g. "review #1", "prototype the top Planned item").

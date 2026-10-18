---
name: run-client
description: Build and launch the RuneLite developer client with Ultimate Bank loaded so the user can test in-game.
---

# Run the dev client

1. `./gradlew build`. Fix any failures first.
2. Launch in the background: `./gradlew run` (it starts RuneLite with `--developer-mode --debug`). Logs also go to `~/.runelite/logs/client.log`.
3. Tell the user:
   - With a Jagex account, the dev client needs credentials exported once. See https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
   - Where to find the plugin: Configuration › "Ultimate Bank"
   - **What to test**: the checklist from the relevant `docs/features/<name>.md`
4. **Never** interact with the game client yourself (no screenshots + clicks, no input automation). That violates Jagex's rules (AGENTS.md › Testing).
5. If the user reports a problem, read the `com.ultimatebank` lines in `~/.runelite/logs/client.log` first.

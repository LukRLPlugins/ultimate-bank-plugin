---
name: hub-check
description: Audit the plugin against RuneLite Plugin Hub rules and the submission checklist before a release or Hub PR.
---

# Plugin Hub audit

Report each item as a pass, a fail, or a warning, with `file:line` for each problem. Don't fix anything until the user asks.

1. **Build.** `./gradlew clean build` passes.
2. **Forbidden APIs.** Grep `src/main` for:
   - reflection: `java.lang.reflect`, `getDeclaredField`, `getDeclaredMethod`, `setAccessible`, `Class.forName`
   - native/process: `Runtime.getRuntime`, `ProcessBuilder`, `System.loadLibrary`, `sun.misc.Unsafe`, `jna`, `lwjgl`
   - dynamic code: `ClassLoader`, `defineClass`, `ScriptEngine`
   - serialization: `ObjectInputStream`, `ObjectOutputStream`, `implements Serializable`
   - input: `java.awt.Robot`, `dispatchEvent`, `KeyEvent(`, `MouseEvent(`
   - HTTP outside OkHttp: `HttpURLConnection`, `java.net.http`; `new Gson(`; `java.awt.Desktop`; `Thread.sleep`; `JFileChooser`
   - raw file I/O outside `Filepath`: `new File(`, `Files.write`, `FileOutputStream`
3. **Menus/interface.** Any `MenuEntry` additions or removals and any widget hiding/unhiding: check them against the AGENTS.md menu and interface restrictions.
4. **Network.** Any OkHttp usage must be behind an opt-in toggle with the exact third-party warning text from AGENTS.md.
5. **Lifecycle.** Every feature's `shutDown()` undoes its `startUp()` (overlays, registered tags, widget changes, executors).
6. **Config.** Compare config keys to the last release tag (`git diff <tag> -- src/main/java/com/ultimatebank/UltimateBankConfig.java`). Any renamed or removed key needs a migration.
7. **Packaging.** No `com.example` leftovers, no `META-INF/services`, `.gitignore` covers build output, `build.gradle` still matches the template, `runelite-plugin.properties` is complete, the README is current, and any icon.png is ≤ 48x72.
8. **Identity.** No real-name leaks: grep the working tree and `git log --all --format='%an %ae %cn %ce'` for anything
   other than Luk (see CLAUDE.md › Identity). Any hit is a blocker.
9. **Attribution.** Every ported feature in `docs/roadmap.md` has a row in `docs/attribution.md`.
10. Then walk through `docs/plugin-hub-submission.md` and list what's left before submitting.

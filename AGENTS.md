# AGENTS.md

## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Five-role vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`.

### Domain docs

`GLOSSARY.md` at the repo root, and decisions in `docs/adr/`. Use their vocabulary in issues, proposals, hypotheses and test names. More are created lazily via `/domain-modeling` when terms or decisions actually get resolved.

## Pre-flight

Before touching source: `git status` for uncommitted work, read `.scratch/` for active specs, `grep` for callers before changing a public method.
After any edit in `src/main/java`, expect `gradle compileJava` or `gradle test` before calling it done.

## Architecture review trigger

Flag for `/improve-codebase-architecture` when any `src/main/java/**/*.java` file grows past ~150 lines or gains >2 public methods with the same noun; also when a decision and the code that applies it end up in the same place — a name literal compared to find a passive in the server entry, or the screen reading what a card line says in order to draw it. Decisions stay pure and adapters apply them; a decision that needs a game object has the seam in the wrong place. See ADR-0001.

## Build

Use system `gradle`, not `./gradlew` (wrapper stalls in agent shell). PowerShell-safe commands only.

Client-visible changes ship with `gradle deployClient`: it rebuilds, copies the jar to the live instance `mods/` folder (override with `-PmcModsDir=<path>` or `MC_MODS_DIR`), and fails if the jar is missing. Never declare a client change done on `compileJava` alone — it never refreshes the jar. Reopen the picker in-game with `/class select <player>` instead of fresh worlds.

## Shell notes (Windows)

`Select-Object -First N` (no `-FirstLines`). No `python3` assume — inspect jars with .NET (`System.IO.Compression.ZipFile`) or Gradle.
PowerShell-safe tail/head: `Select-Object -First N` / `Select-Object -Last N`; use `2>$null` instead of `/dev/null`, never bare `| tail` or `| head`.
Edit files with the edit tool; never rewrite file contents through the shell (redirection, `Set-Content`, `Out-File`).
Resolve Fabric/vanilla signatures with `javap` against jars under `~/.gradle/caches/modules-2` (javap lives in the JDK's `bin`; quote the spaces).
Target is Minecraft 1.21.11: prefer `Button` widgets over `Screen` input/tooltip overrides — the 1.21 input and tooltip signatures drifted and Buttons compile first try.

# AGENTS.md

## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Five-role vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`.

### Domain docs

None yet (`GLOSSARY.md`, `docs/adr/`). Created lazily via `/domain-modeling` when terms or decisions actually get resolved.

## Build

Use system `gradle`, not `./gradlew` (wrapper stalls in agent shell). PowerShell-safe commands only.

Client-visible changes ship with `gradle deployClient`: it rebuilds, copies the jar to the live instance `mods/` folder (override with `-PmcModsDir=<path>` or `MC_MODS_DIR`), and fails if the jar is missing. Never declare a client change done on `compileJava` alone — it never refreshes the jar. Reopen the picker in-game with `/class select <player>` instead of fresh worlds.

## Shell notes (Windows)

`Select-Object -First N` (no `-FirstLines`). No `python3` assume — inspect jars with .NET (`System.IO.Compression.ZipFile`) or Gradle.
Edit files with the edit tool; never rewrite file contents through the shell (redirection, `Set-Content`, `Out-File`).
Resolve Fabric/vanilla signatures with `javap` against jars under `~/.gradle/caches/modules-2` (javap lives in the JDK's `bin`; quote the spaces).
Target is Minecraft 1.21.11: prefer `Button` widgets over `Screen` input/tooltip overrides — the 1.21 input and tooltip signatures drifted and Buttons compile first try.

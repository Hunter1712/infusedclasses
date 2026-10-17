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
After any edit in `src/main/java`, expect `./gradlew compileJava` or `./gradlew test` before calling it done.

## Architecture review trigger

Flag for `/improve-codebase-architecture` when any `src/main/java/**/*.java` file grows past ~150 lines or gains >2 public methods with the same noun; also when a decision and the code that applies it end up in the same place — a name literal compared to find a passive in the server entry, or the screen reading what a card line says in order to draw it. Decisions stay pure and adapters apply them; a decision that needs a game object has the seam in the wrong place. See ADR-0001.

## Build

Use `./gradlew` (wrapper, Gradle 9.7.1; first run downloads the distribution). Commands run over bash.

Client-visible changes ship with `./gradlew deployClient`: it rebuilds, copies the jar to the live instance `mods/` folder (default: the Linux PrismLauncher `1.21.11` instance; override with `-PmcModsDir=<path>` or `MC_MODS_DIR`), and fails if the jar is missing. Never declare a client change done on `compileJava` alone — it never refreshes the jar. Reopen the picker in-game with `/class select <player>` instead of fresh worlds.

## Shell notes (Linux)

Bash — plain `head`, `tail`, `grep` for output; inspect jars with `unzip -l` or `jar tf`; `python3` is available.
Edit files with the edit tool; never rewrite file contents through the shell (redirection, `tee`, `sed -i`).
Resolve Fabric/vanilla signatures with `javap`: the merged jar lives under `~/.gradle/caches/fabric-loom/minecraftMaven` (glob `minecraft-merged-1.21.11-*.jar`, the hash rots). The JDK lives in `~/.local/share/jdks/jdk-25.0.4.1`, with `java`/`javac`/`jar`/`javap` symlinked into `/usr/local/bin`.
Target is Minecraft 1.21.11: prefer `Button` widgets over `Screen` input/tooltip overrides — the 1.21 input and tooltip signatures drifted and Buttons compile first try.

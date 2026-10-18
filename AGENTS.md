# AGENTS.md

## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Five-role vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`.

### Domain docs

`GLOSSARY.md` at the repo root, and decisions in `docs/adr/` (index: `docs/adr/README.md`). Use their vocabulary in issues, proposals, hypotheses and test names. More are created lazily via `/domain-modeling` when terms or decisions actually get resolved.

### Review standards

`CODING_STANDARDS.md` holds the judgement calls a reviewer enforces, plus the round-review convention. Reviewers read it with `GLOSSARY.md` and the ADRs a change touches.

## Pre-flight

Before touching source: `git status` for uncommitted work, read `.scratch/` for active specs, `grep` for callers before changing a public method.
After any edit in `src/main/java`, expect `./gradlew compileJava` or `./gradlew test` before calling it done. `tools/doc-flags.sh` before calling a round done flags docs drift: a changed class named in `GLOSSARY.md` with the glossary untouched, or no `CHANGELOG.md` touch at all.

## Architecture review trigger

Flag for `/improve-codebase-architecture` when any `src/main/java/**/*.java` file grows past ~150 lines or gains >2 public methods with the same noun; run `tools/arch-flags.sh` before calling a round done to see each changed file's before -> after count against that line; also when a decision and the code that applies it end up in the same place — a name literal compared to find a passive in the server entry, or the screen reading what a card line says in order to draw it. Decisions stay pure and adapters apply them; a decision that needs a game object has the seam in the wrong place. See ADR-0001.

## Build

Use `./gradlew` (wrapper, Gradle 9.7.1; first run downloads the distribution). Commands run over bash.

Client-visible changes ship with `./gradlew deployClient`: it rebuilds, copies the jar to the live instance `mods/` folder (default: the ElyPrismLauncher `1.21.11` instance at `~/.local/share/ElyPrismLauncher/instances/1.21.11/minecraft`; override with `-PmcModsDir=<path>` or `MC_MODS_DIR`), and fails if the jar is missing. Never declare a client change done on `compileJava` alone — it never refreshes the jar. Reopen the picker in-game with `/class select <player>` instead of fresh worlds, then read that instance's `logs/latest.log` — grep it for `infusedclasses` or exceptions before trusting a client-visible change.

Two vanilla contracts the mixins bend break silently — the build stays green either way — so a round that touches melee is not done until both are seen in game: a full-charge spear thrust earns like a sword (Momentum bump, crit roll, Rush credit), and a hit fully eaten by a raised shield earns nothing while Rush, Impetus and Rend stay pending. When in doubt about the vanilla shape, `javap` the merged jar before trusting the capture.

The `DamageSettle` guarded hurt needs the same hand check: a bonus smaller than the hit itself (a x1.25 crit on a wood sword, a Rend spend) lands only when the `lastHurt` memory is cleared around it, so a round that touches the settle must see the target's health drop by hit plus bonus, with one sound, hurt flash and knockback, and later hits still comparing against the combined total.

## Shell notes (Linux)

Bash — plain `head`, `tail`, `grep` for output; inspect jars with `unzip -l` or `jar tf`; `python3` is available.
Edit files with the edit tool; never rewrite file contents through the shell (redirection, `tee`, `sed -i`).
Resolve Fabric/vanilla signatures with `javap`: the merged jar lives under `~/.gradle/caches/fabric-loom/minecraftMaven` (glob `minecraft-merged-1.21.11-*.jar`, the hash rots), and `tools/mc-method.sh <class> <method>` prints one method body out of it. After a build that adds or edits a mixin, run `tools/mixin-targets.sh`: it resolves the built jar's annotation targets from intermediary ids (`method_75123`) to named members via `~/.gradle/caches/fabric-loom/1.21.11/loom.mappings.*/mappings.tiny`. The JDK lives in `~/.local/share/jdks/jdk-25.0.4.1`, with `java`/`javac`/`jar`/`javap` symlinked into `/usr/local/bin`.
Target is Minecraft 1.21.11: prefer `Button` widgets over `Screen` input/tooltip overrides — the 1.21 input and tooltip signatures drifted and Buttons compile first try.

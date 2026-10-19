# Spec: Retro tools — merged-jar resolver, sound resolver, ADR index check

Status: done

## Goal

The Oct 19 2026 retro found three environment gaps in this session: every round
hand-rolls the Loom merged-jar path (28+ hand-built lookups in one session, in four
shapes, one of them broken), sound work re-derives the launcher's asset index by hand,
and `tools/doc-flags.sh` watches GLOSSARY/CHANGELOG but not the ADR index. This round
gives each gap one resolver. No `src/main/java` change.

## Decisions

- **One jar resolver.** `tools/mc-jar.sh` prints the newest Loom merged jar for
  `minecraft_version` (`--client` prints the client jar); it exits with the "run
  ./gradlew classes once" message when the cache is cold. `tools/mc-method.sh` uses it,
  so the mappings-hash lookup lives in one place instead of five.
- **One sound resolver.** `tools/mc-sound.sh <event-id>` walks the launcher assets store
  (`MC_ASSETS_DIR` overrides the ElyPrismLauncher default) newest index first, so a
  1.17-era event cannot shadow its renamed 1.21 form; it follows `{"type": "event"}`
  references and prints each variant's ogg object path with its size.
- **doc-flags watches the index.** A new `docs/adr/NNNN-*.md` with `docs/adr/README.md`
  untouched, and any changed ADR whose number is absent from that index, are flagged in
  the same voice as the existing drift flags — still advisory, always exit 0.
- **AGENTS.md points at the tools.** The Shell notes name `mc-jar.sh` instead of warning
  about the rotting glob, and name the launcher assets layout plus `mc-sound.sh` instead
  of leaving them to be rediscovered.

## Testing

No shell test harness exists in the repo; verification is the scenario matrix below,
run in the live repo and in a scratch clone for the doc-flags paths.

- `tools/mc-jar.sh` and `--client` print existing jars; `--bogus` exits 2.
- `tools/mc-method.sh` still prints a method body after the refactor.
- `tools/mc-sound.sh entity.evoker.cast_spell` prints two variants whose paths exist;
  `entity.guardian.ambient` resolves from the newest index (`mob/guardian/guardian_idle*`,
  not the 1.17 `entity/guardian/ambient*` names); `block.note_block.imitate.creeper`
  follows its event reference; an unknown event exits 1; a bad `MC_ASSETS_DIR` exits 1.
- `tools/doc-flags.sh` in a clone: clean tree → no-change message; new unindexed ADR →
  both ADR flags; indexed ADR with README touched → silent; changed ADR whose index line
  was removed → missing-from-index flag; src change with untouched GLOSSARY/CHANGELOG →
  the existing two flags.

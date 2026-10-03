# AGENTS.md

## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout (`GLOSSARY.md` + `docs/adr/` at root). See `docs/agents/domain.md`.

### Build

Use system `gradle`, not `./gradlew` (wrapper stalls in agent shell). PowerShell-safe commands only.

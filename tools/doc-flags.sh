#!/usr/bin/env bash
# Flags a round whose docs probably lag its code:
#   - a changed src/main/java class named in GLOSSARY.md, with GLOSSARY.md untouched
#   - any src/main/java change with CHANGELOG.md untouched
#   - a new docs/adr/NNNN-*.md with docs/adr/README.md untouched, or a number
#     missing from that index
# Heuristic, always exits 0: the flags are for the round's author to read.
set -euo pipefail
cd "$(dirname "$0")/.."

touched() { git status --porcelain -- "$1" | grep -q .; }

found=0

# ADR bookkeeping: a new ADR lands with its index line in the same change, and
# any changed ADR keeps its number in the index (docs/adr/README.md).
new_adrs=$(git status --porcelain -- 'docs/adr' \
	| awk '$1 ~ /^(\?\?|A)/ {print $2}' \
	| grep -E '^docs/adr/[0-9]{4}-.*\.md$' || true)
if [ -n "$new_adrs" ] && ! touched docs/adr/README.md; then
	echo "doc-flags: new ADR(s) in the round and docs/adr/README.md is untouched"
	found=1
fi
adrs=$(git status --porcelain -- 'docs/adr' \
	| awk '{print $2}' \
	| grep -E '^docs/adr/[0-9]{4}-.*\.md$' || true)
if [ -n "$adrs" ]; then
	while IFS= read -r adr; do
		number=$(basename "$adr" | cut -c1-4)
		if ! grep -q -- "$number-" docs/adr/README.md; then
			echo "doc-flags: $adr is missing from the docs/adr/README.md index"
			found=1
		fi
	done <<< "$adrs"
fi

changed=$(
	{
		git diff --name-only HEAD -- 'src/main/java'
		git ls-files --others --exclude-standard -- 'src/main/java'
	} | sort -u
)
if [ -z "$changed" ]; then
	if [ "$found" -eq 0 ]; then
		echo "doc-flags: no src/main/java changes in the round"
	fi
	exit 0
fi

if ! touched GLOSSARY.md; then
	while IFS= read -r file; do
		class=$(basename "$file" .java)
		if grep -qE "\b$class\b" GLOSSARY.md; then
			echo "doc-flags: $class changed and named in GLOSSARY.md, but GLOSSARY.md is untouched"
			found=1
		fi
	done <<< "$changed"
fi

if ! touched CHANGELOG.md; then
	echo "doc-flags: src/main/java changed and CHANGELOG.md is untouched (fine for a refactor, expected for a player-visible change)"
	found=1
fi

if [ "$found" -eq 0 ]; then
	echo "doc-flags: no GLOSSARY/CHANGELOG drift to flag"
fi

#!/usr/bin/env bash
# Flags a round whose docs probably lag its code:
#   - a changed src/main/java class named in GLOSSARY.md, with GLOSSARY.md untouched
#   - any src/main/java change with CHANGELOG.md untouched
# Heuristic, always exits 0: the flags are for the round's author to read.
set -euo pipefail
cd "$(dirname "$0")/.."

changed=$(
	{
		git diff --name-only HEAD -- 'src/main/java'
		git ls-files --others --exclude-standard -- 'src/main/java'
	} | sort -u
)
if [ -z "$changed" ]; then
	echo "doc-flags: no src/main/java changes in the round"
	exit 0
fi

touched() { git status --porcelain -- "$1" | grep -q .; }

found=0

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

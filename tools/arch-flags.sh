#!/usr/bin/env bash
# Prints the round's changed src/main/java files that sit over the
# architecture-review line in AGENTS.md (~150), so growth is visible while the
# round is still open. Always exits 0: the line is a flag, not a gate.
set -euo pipefail
cd "$(dirname "$0")/.."

THRESHOLD="${ARCH_FLAGS_THRESHOLD:-150}"
found=0

while IFS= read -r file; do
	[ -f "$file" ] || continue
	lines=$(wc -l < "$file")
	if [ "$lines" -gt "$THRESHOLD" ]; then
		printf '%5d  %s\n' "$lines" "$file"
		found=1
	fi
done < <(
	{
		git diff --name-only HEAD -- 'src/main/java'
		git ls-files --others --exclude-standard -- 'src/main/java'
	} | sort -u
)

if [ "$found" -eq 0 ]; then
	echo "arch-flags: no changed src/main/java file over $THRESHOLD lines"
fi

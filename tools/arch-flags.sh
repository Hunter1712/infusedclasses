#!/usr/bin/env bash
# Prints the round's changed src/main/java files that sit over the
# architecture-review line in AGENTS.md (~150), each with its line count before
# the round ("new" when untracked), so growth is visible while the round is
# still open. Always exits 0: the line is a flag, not a gate.
set -euo pipefail
cd "$(dirname "$0")/.."

THRESHOLD="${ARCH_FLAGS_THRESHOLD:-150}"
found=0

while IFS= read -r file; do
	[ -f "$file" ] || continue
	lines=$(wc -l < "$file")
	if [ "$lines" -gt "$THRESHOLD" ]; then
		if git cat-file -e "HEAD:$file" 2>/dev/null; then
			before=$(git show "HEAD:$file" | wc -l)
		else
			before=new
		fi
		printf '%5s -> %5d  %s\n' "$before" "$lines" "$file"
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

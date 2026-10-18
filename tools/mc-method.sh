#!/usr/bin/env bash
# Prints one method body out of the merged named Minecraft jar, so vanilla
# signatures are read without re-deriving the Loom cache paths by hand.
# Usage: tools/mc-method.sh <class> <method>
#        tools/mc-method.sh net.minecraft.world.entity.player.Player stabAttack
# Prints the first matching overload. Exits 1 when the class or method is absent.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ $# -ne 2 ]; then
	echo "usage: tools/mc-method.sh <class> <method>" >&2
	exit 2
fi

class="$1"
method="$2"

mcver=$(sed -n 's/^minecraft_version=//p' gradle.properties)
[ -n "$mcver" ] || { echo "mc-method: no minecraft_version in gradle.properties" >&2; exit 1; }

# The mappings hash in the path rots; take the newest jar for the target version.
jar=$(ls -t \
	"$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/${mcver}"-*/minecraft-merged-*.jar \
	2>/dev/null | head -1 || true)
if [ -z "$jar" ]; then
	echo "mc-method: no merged jar for $mcver in the Loom cache — run ./gradlew classes once" >&2
	exit 1
fi

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

classfile="${class//.//}.class"
if ! unzip -qo "$jar" "$classfile" -d "$tmp" 2>/dev/null; then
	echo "mc-method: $class is not in $(basename "$jar")" >&2
	exit 1
fi

if ! body=$(javap -c -p -cp "$tmp" "$class" | awk -v name="$method" '
	!printing && index($0, " " name "(") { printing = 1; print; next }
	printing && /^  [^ ]/ { exit }
	printing { print }
	END { if (!printing) exit 1 }
'); then
	echo "mc-method: no method $method in $class" >&2
	exit 1
fi

printf '%s\n' "$body"

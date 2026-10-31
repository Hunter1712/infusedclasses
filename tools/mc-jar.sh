#!/usr/bin/env bash
# Prints the Loom-cached Minecraft jars, so javap/unzip calls never re-derive
# the cache paths by hand (the mappings hash in the merged path rots).
# Usage: tools/mc-jar.sh [<version>] [--client]
#   default:         the newest merged named jar for a supported version
#   <version>:       the target's own game version (e.g. 1.21.1); defaults to
#                    each version module's declared minecraft_version
#   --client:        the client jar
# Exits 1 when the jar is missing from the cache (run ./gradlew classes once).
set -euo pipefail
cd "$(dirname "$0")/.."

kind="merged"
tool_version=""
while [ $# -gt 0 ]; do
	case "$1" in
	--client) kind="client" ;;
	"") ;;
	*) tool_version="$1" ;;
	esac
	shift
done

mcver="${tool_version:-$(sed -n 's/^minecraft_version=//p' versions/*/gradle.properties 2>/dev/null | head -1)}"
[ -n "$mcver" ] || { echo "mc-jar: no minecraft_version in a version module's gradle.properties" >&2; exit 1; }

case "$kind" in
merged)
	# Take the newest jar for the target version.
	jar=$(ls -t \
		"$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/${mcver}"-*/minecraft-merged-*.jar \
		2>/dev/null | head -1 || true)
	;;
client)
	jar="$HOME/.gradle/caches/fabric-loom/${mcver}/minecraft-client.jar"
	;;
esac

if [ -z "$jar" ] || [ ! -f "$jar" ]; then
	echo "mc-jar: no $kind jar for $mcver in the Loom cache — run ./gradlew ${tool_version:+:mc$(echo "$tool_version" | tr -d .)}classes once" >&2
	exit 1
fi
printf '%s\n' "$jar"

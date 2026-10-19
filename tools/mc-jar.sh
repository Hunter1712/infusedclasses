#!/usr/bin/env bash
# Prints the Loom-cached Minecraft jars, so javap/unzip calls never re-derive
# the cache paths by hand (the mappings hash in the merged path rots).
# Usage: tools/mc-jar.sh [--client]
#   default:  the newest merged named jar for minecraft_version — classes and data files
#   --client: the client jar
# Exits 1 when the jar is missing from the cache (run ./gradlew classes once).
set -euo pipefail
cd "$(dirname "$0")/.."

mcver=$(sed -n 's/^minecraft_version=//p' gradle.properties)
[ -n "$mcver" ] || { echo "mc-jar: no minecraft_version in gradle.properties" >&2; exit 1; }

kind="merged"
case "${1:-}" in
"")
	# Take the newest jar for the target version.
	jar=$(ls -t \
		"$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/${mcver}"-*/minecraft-merged-*.jar \
		2>/dev/null | head -1 || true)
	;;
--client)
	kind="client"
	jar="$HOME/.gradle/caches/fabric-loom/${mcver}/minecraft-client.jar"
	;;
*)
	echo "usage: tools/mc-jar.sh [--client]" >&2
	exit 2
	;;
esac

if [ -z "$jar" ] || [ ! -f "$jar" ]; then
	echo "mc-jar: no $kind jar for $mcver in the Loom cache — run ./gradlew classes once" >&2
	exit 1
fi
printf '%s\n' "$jar"

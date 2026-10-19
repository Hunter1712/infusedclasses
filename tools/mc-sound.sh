#!/usr/bin/env bash
# Resolves a vanilla sound event to its ogg files, so sound work reads the
# launcher's asset store instead of re-deriving the asset-index hashes by hand.
# Usage: tools/mc-sound.sh <event-id>
#        tools/mc-sound.sh entity.evoker.cast_spell
# Reads the launcher assets dir (default: the ElyPrismLauncher one, override
# with MC_ASSETS_DIR); walks the asset indexes newest-first, follows
# {"type": "event"} references, and prints one line per ogg variant. Exits 1
# when the event is absent and 2 on a usage error.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ $# -ne 1 ]; then
	echo "usage: tools/mc-sound.sh <event-id>" >&2
	exit 2
fi

assets="${MC_ASSETS_DIR:-$HOME/.local/share/ElyPrismLauncher/assets}"
if [ ! -d "$assets/indexes" ] || [ ! -d "$assets/objects" ]; then
	echo "mc-sound: no asset store at $assets (set MC_ASSETS_DIR)" >&2
	exit 1
fi

python3 - "$assets" "$1" <<'PY'
import json, os, re, sys

assets, event = sys.argv[1], sys.argv[2]


def object_path(entry):
    return os.path.join(assets, "objects", entry["hash"][:2], entry["hash"])


def variants(event_id, events, seen):
    for variant in events[event_id].get("sounds", []):
        if isinstance(variant, dict) and variant.get("type") == "event":
            ref = variant.get("name")
            if ref in events and ref not in seen:
                yield from variants(ref, events, seen | {ref})
        else:
            yield variant


index_names = [n for n in os.listdir(os.path.join(assets, "indexes")) if n.endswith(".json")]
# Newest first: lexicographic order would pick 17.json before 34.json.
index_names.sort(key=lambda n: int(re.sub(r"\D", "", n) or 0), reverse=True)

for index_name in index_names:
    with open(os.path.join(assets, "indexes", index_name)) as f:
        index = json.load(f).get("objects", {})
    sounds_entry = index.get("minecraft/sounds.json")
    if not sounds_entry:
        continue
    sounds_path = object_path(sounds_entry)
    if not os.path.exists(sounds_path):
        continue
    with open(sounds_path) as f:
        events = json.load(f)
    if event not in events:
        continue
    for variant in variants(event, events, {event}):
        name = variant if isinstance(variant, str) else variant.get("name")
        ogg_entry = index.get("minecraft/sounds/%s.ogg" % name) if name else None
        if not ogg_entry:
            print("%-44s no ogg in this index" % name)
            continue
        path = object_path(ogg_entry)
        note = "" if os.path.exists(path) else " (not downloaded)"
        print("%-44s %8d%s %s" % (name, ogg_entry["size"], note, path))
    sys.exit(0)

sys.exit("mc-sound: %s is not in the assets index" % event)
PY

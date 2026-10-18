#!/usr/bin/env bash
# Resolves the built mixin jar's vanilla targets from their intermediary ids
# (method_75123) back to named members, so a wrong @Inject/@Accessor target is
# readable before launch. Exits 1 when a target did not remap or does not resolve.
# Usage: tools/mixin-targets.sh [jar]
#        (default: the newest build/libs/infusedclasses-*.jar)
set -euo pipefail
cd "$(dirname "$0")/.."

if [ $# -ge 1 ]; then
	jar="$1"
else
	jar=$(ls -t build/libs/infusedclasses-*.jar 2>/dev/null | head -1 || true)
fi
if [ -z "$jar" ] || [ ! -f "$jar" ]; then
	echo "mixin-targets: no jar under build/libs — run ./gradlew build first" >&2
	exit 1
fi

mcver=$(sed -n 's/^minecraft_version=//p' gradle.properties)
map=$(ls -t "$HOME/.gradle/caches/fabric-loom/$mcver"/loom.mappings.*/mappings.tiny 2>/dev/null | head -1 || true)
if [ -z "$map" ]; then
	echo "mixin-targets: no Loom mappings for $mcver — run ./gradlew classes once" >&2
	exit 1
fi

JAR="$jar" MAP="$map" python3 - <<'PY'
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

jar = os.environ["JAR"]
map_path = os.environ["MAP"]

# Tiny v2: "c <official> <intermediary> <named>" and, indented under a class,
# "m/f <officialDesc> <official> <intermediary> <named>".
methods, fields, classes = {}, {}, {}
with open(map_path, encoding="utf-8") as fh:
    for line in fh:
        parts = [p for p in line.rstrip("\n").split("\t") if p]
        if len(parts) >= 5 and parts[0] == "m":
            methods[parts[3]] = parts[4]
        elif len(parts) >= 5 and parts[0] == "f":
            fields[parts[3]] = parts[4]
        elif len(parts) >= 4 and parts[0] == "c":
            # Intermediary class names carry the package; annotation tokens are bare.
            classes[parts[2].rsplit("/", 1)[-1]] = parts[3]

REF_KEYS = {
    "injection.Inject": ("method",),
    "injection.ModifyArg": ("method",),
    "injection.ModifyArgs": ("method",),
    "injection.ModifyVariable": ("method", "name"),
    "injection.Redirect": ("method",),
    "injection.At": ("target",),
    "gen.Accessor": ("value",),
    "gen.Invoker": ("value",),
    "Mixin": ("value", "targets"),
}
TOKEN = re.compile(r"\b((?:method|field|class)_\d+)\b")
OPEN = re.compile(r"^\s+(org\.spongepowered\.asm\.mixin\.[\w.]+)\($")
MEMBER = re.compile(r"^\s{2}\S.*\(.*\);$")
KEY = re.compile(r"\s*([A-Za-z0-9_]+)=")

problems = 0
refs = 0


def emit(suffix, member, buf, rows, targets):
    global problems, refs
    who = "-" if suffix == "Mixin" else member
    text = " ".join(line.strip() for line in buf)
    if suffix == "Mixin":
        for tok in TOKEN.findall(text):
            if not tok.startswith("class_"):
                continue
            hit = classes.get(tok)
            if hit is None:
                row = f"  {who}  @{suffix} {tok} -> UNRESOLVED"
                if row not in rows:
                    rows.append(row)
                    problems += 1
            elif hit not in targets:
                targets.append(hit)
    else:
        for line in buf:
            m = KEY.match(line)
            key = m.group(1) if m else "?"
            for tok in TOKEN.findall(line):
                if tok.startswith("method_"):
                    hit = methods.get(tok)
                elif tok.startswith("field_"):
                    hit = fields.get(tok)
                else:
                    hit = classes.get(tok)
                if hit is None:
                    row = f"  {who}  @{suffix} {key}: {tok} -> UNRESOLVED"
                else:
                    row = f"  {who}  @{suffix} {key}: {tok} -> {hit}"
                if row not in rows:
                    rows.append(row)
                    if hit is None:
                        problems += 1
                    else:
                        refs += 1
    for key in REF_KEYS.get(suffix, ()):
        m = re.search(r"\b" + key + r'\s*=\s*(?:"([^"]*)"|\[([^\]]*)\])', text)
        if not m:
            continue
        val = m.group(1) if m.group(1) is not None else m.group(2)
        if val and "*" not in val and not TOKEN.search(val):
            row = f"  {who}  @{suffix} {key}={val} -> not an intermediary ref (remap failed?)"
            if row not in rows:
                rows.append(row)
                problems += 1


zf = zipfile.ZipFile(jar)
mod = json.loads(zf.read("fabric.mod.json"))
mixins = []
for entry in mod.get("mixins", []):
    cfg_name = entry if isinstance(entry, str) else entry.get("config", "")
    if not cfg_name:
        continue
    cfg = json.loads(zf.read(cfg_name))
    package = cfg.get("package", "")
    for slot in ("mixins", "client", "server"):
        for m in cfg.get(slot, []) or []:
            name = m if isinstance(m, str) else m.get("name", "")
            if name:
                mixins.append(f"{package}.{name}" if package else name)

if not mixins:
    print(f"mixin-targets: no mixin configs in {jar}")
    sys.exit(0)

tmp = tempfile.mkdtemp()
print(f"mixin-targets: {jar}")
for fqcn in mixins:
    path = fqcn.replace(".", "/") + ".class"
    try:
        blob = zf.read(path)
    except KeyError:
        print(f"{fqcn.rsplit('.', 1)[-1]}: not in jar — rebuild?")
        problems += 1
        continue
    classfile = os.path.join(tmp, path)
    os.makedirs(os.path.dirname(classfile), exist_ok=True)
    with open(classfile, "wb") as fh:
        fh.write(blob)
    out = subprocess.run(
        ["javap", "-v", "-p", "-cp", tmp, fqcn], capture_output=True, text=True
    ).stdout

    rows, targets = [], []
    member = "?"
    ann, buf, depth = None, [], 0
    for line in out.splitlines():
        if ann is None:
            if MEMBER.match(line):
                m = re.search(r"([\w$<>]+)\(", line)
                if m:
                    member = m.group(1)
            m = OPEN.match(line)
            if m:
                ann = m.group(1).removeprefix("org.spongepowered.asm.mixin.")
                buf, depth = [], 1
            continue
        depth += line.count("(") - line.count(")")
        if depth > 0:
            buf.append(line)
        else:
            emit(ann, member, buf, rows, targets)
            ann = None

    header = " -> ".join(dict.fromkeys(targets)) if targets else "?"
    print(f"{fqcn.rsplit('.', 1)[-1]} -> {header}")
    for row in rows:
        print(row)

shutil.rmtree(tmp, ignore_errors=True)

if problems:
    print(f"mixin-targets: {refs} resolved, {problems} problem(s)")
    sys.exit(1)
print(f"mixin-targets: {refs} targets resolved")
PY

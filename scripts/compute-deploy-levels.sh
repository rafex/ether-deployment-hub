#!/usr/bin/env bash
# compute-deploy-levels.sh — Compute parallel deploy levels from a release plan.
#
# Reads release-plan.json and groups modules into topological levels where
# modules at the same level have no inter-dependencies and can be deployed
# in parallel.
#
# Large levels are automatically subdivided so that no single level exceeds
# MAX_LEVEL_SIZE modules. This prevents GitHub Actions job timeouts (30 min)
# when many independent modules land in the same topological wave.
#
# Usage: ./compute-deploy-levels.sh <release-plan.json> [max-per-level]
#   max-per-level  Maximum modules per level (default: 5)
#
# Output (stdout): JSON of the form:
#   {
#     "levels": [
#       ["ether-parent"],
#       ["ether-config", "ether-json", ...],
#       ...
#     ]
#   }
#
# Exit codes:
#   0 — success
#   1 — bad arguments or plan file not found

set -euo pipefail

PLAN_PATH="${1:?Usage: $0 <release-plan.json> [max-per-level]}"
MAX_LEVEL_SIZE="${2:-5}"

if [ ! -f "$PLAN_PATH" ]; then
  echo "Release plan not found: $PLAN_PATH" >&2
  exit 1
fi

python3 - "$PLAN_PATH" "$MAX_LEVEL_SIZE" <<'PYEOF'
import json
import sys

with open(sys.argv[1]) as f:
    plan = json.load(f)

max_level_size = int(sys.argv[2])
preferred_order = plan.get("deployOrder") or [m["name"] for m in plan.get("modules", [])]
preferred_index = {name: idx for idx, name in enumerate(preferred_order)}

# Only modules that are actually being released
releasable = [
    m for m in plan.get("modules", [])
    if m.get("releaseLevel", "none") != "none"
]

release_names = {m["name"] for m in releasable}

# Dependency map: only ether-* deps that are also in the release set
dep_map: dict[str, list[str]] = {
    m["name"]: [
        d for d in (m.get("dependencies") or [])
        if d in release_names
    ]
    for m in releasable
}

# Kahn's algorithm — assign each module to a topological level
topo_levels: list[list[str]] = []
assigned: dict[str, int] = {}
remaining = set(dep_map.keys())

while remaining:
    # Modules whose all deps are already assigned
    current_level = [
        name for name in remaining
        if all(d in assigned for d in dep_map[name])
    ]
    if not current_level:
        # Safety valve: cycle or unresolvable — dump all remaining while still
        # preserving the manifest/release-plan preferred deploy order.
        current_level = list(remaining)

    current_level.sort(key=lambda name: (preferred_index.get(name, len(preferred_index)), name))

    level_idx = len(topo_levels)
    for name in current_level:
        assigned[name] = level_idx

    topo_levels.append(current_level)
    remaining -= set(current_level)

# Split any level that exceeds max_level_size into consecutive sub-levels.
# Modules within a topological level share no inter-dependencies, so any
# arbitrary partition preserves correctness.
final_levels: list[list[str]] = []
for level in topo_levels:
    chunk_start = 0
    while chunk_start < len(level):
        final_levels.append(level[chunk_start : chunk_start + max_level_size])
        chunk_start += max_level_size

print(json.dumps({"levels": final_levels}, indent=2))
PYEOF

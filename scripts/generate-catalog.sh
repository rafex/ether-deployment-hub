#!/usr/bin/env bash
# generate-catalog.sh — merges manifest.json + maven-central-status.json + descriptions.json
# Output: docs/catalog.json
# Usage: ./scripts/generate-catalog.sh
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MANIFEST="${ROOT_DIR}/releases/manifest.json"
STATUS="${ROOT_DIR}/docs/maven-central-status.json"
DESCRIPTIONS="${ROOT_DIR}/docs/descriptions.json"
OUTPUT="${ROOT_DIR}/docs/catalog.json"

echo "→ Generating docs/catalog.json ..."

python3 << PYEOF
import json, sys, datetime
from pathlib import Path

manifest_path  = "${MANIFEST}"
status_path    = "${STATUS}"
descs_path     = "${DESCRIPTIONS}"
output_path    = "${OUTPUT}"

manifest   = json.loads(Path(manifest_path).read_text())
status_raw = json.loads(Path(status_path).read_text())
descs_raw  = json.loads(Path(descs_path).read_text())

status_by_name = {s["module"]: s for s in status_raw}
descs          = descs_raw.get("descriptions", {})

modules_out = []
for mod in manifest.get("modules", []):
    name = mod["name"]
    s    = status_by_name.get(name, {})
    modules_out.append({
        "name":                    name,
        "groupId":                 mod.get("groupId",    s.get("groupId",    "")),
        "artifactId":              mod.get("artifactId", s.get("artifactId", name)),
        "version":                 s.get("latestVersion") or mod.get("currentVersion", ""),
        "deployed":                s.get("deployed", mod.get("published", False)),
        "description":             descs.get(name, ""),
        "dependencies":            mod.get("dependencies", []),
        "dependencyReleaseImpact": mod.get("dependencyReleaseImpact", "patch"),
        "artifactUrl":             s.get("artifactUrl", ""),
        "mavenCoord":              f"{mod.get('groupId','')}:{mod.get('artifactId', name)}",
    })

catalog = {
    "schemaVersion": 1,
    "generatedAt":   datetime.datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%SZ"),
    "totalModules":  len(modules_out),
    "deployOrder":   manifest.get("deployOrder", []),
    "releasePolicy": manifest.get("releasePolicy", {}),
    "modules":       modules_out,
}

Path(output_path).write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")
print(f"✓ Written {len(modules_out)} modules → {output_path}")
PYEOF

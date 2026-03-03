#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT_DIR/docs"
JSON_FILE="$OUT_DIR/maven-central-status.json"
TABLE_FILE="$OUT_DIR/maven-central-status-table.md"

mkdir -p "$OUT_DIR"

MODULES=(
  "ether-parent|dev.rafex.ether.parent|ether-parent"
  "ether-json|dev.rafex.ether.json|ether-json"
  "ether-jwt|dev.rafex.ether.jwt|ether-jwt"
  "ether-http-core|dev.rafex.ether.http|ether-http-core"
  "ether-http-jetty12|dev.rafex.ether.http|ether-http-jetty12"
)

TMP_JSON="$(mktemp)"
echo "[]" > "$TMP_JSON"

for row in "${MODULES[@]}"; do
  IFS="|" read -r module group artifact <<< "$row"

  group_path="$(echo "$group" | tr '.' '/')"
  metadata_url="https://repo1.maven.org/maven2/$group_path/$artifact/maven-metadata.xml"

  if metadata_xml="$(curl -fsS "$metadata_url" 2>/dev/null)"; then
    metadata_line="$(printf '%s' "$metadata_xml" | tr -d '\n')"
    latest_version="$(printf '%s' "$metadata_line" | sed -n 's:.*<release>\([^<]*\)</release>.*:\1:p')"
    if [ -z "$latest_version" ]; then
      latest_version="$(printf '%s' "$metadata_line" | sed -n 's:.*<latest>\([^<]*\)</latest>.*:\1:p')"
    fi
    if [ -z "$latest_version" ]; then
      latest_version="$(printf '%s' "$metadata_line" | sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p')"
    fi

    deployed=true
    last_updated=""
    artifact_url="https://central.sonatype.com/artifact/$group/$artifact"
  else
    deployed=false
    latest_version=""
    last_updated=""
    artifact_url="https://central.sonatype.com/search?q=$artifact"
  fi

  jq \
    --arg module "$module" \
    --arg group "$group" \
    --arg artifact "$artifact" \
    --arg latest_version "$latest_version" \
    --arg last_updated "$last_updated" \
    --arg artifact_url "$artifact_url" \
    --argjson deployed "$deployed" \
    '. + [{
      module: $module,
      groupId: $group,
      artifactId: $artifact,
      deployed: $deployed,
      latestVersion: $latest_version,
      lastUpdatedUtc: $last_updated,
      artifactUrl: $artifact_url
    }]' "$TMP_JSON" > "${TMP_JSON}.next"

  mv "${TMP_JSON}.next" "$TMP_JSON"
done

mv "$TMP_JSON" "$JSON_FILE"

{
  echo "| Modulo | Badge | GroupId | ArtifactId | Desplegado |"
  echo "|---|---|---|---|---|"
  jq -r '
    .[] |
    "| \(.module) | ![\(.module)](https://img.shields.io/maven-central/v/\(.groupId)/\(.artifactId)) | \(.groupId) | \(.artifactId) | " +
    (if .deployed then "si" else "no" end) + " |"
  ' "$JSON_FILE"
} > "$TABLE_FILE"

echo "Generated:"
echo "  - $JSON_FILE"
echo "  - $TABLE_FILE"

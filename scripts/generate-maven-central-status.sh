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

  response="$(
    curl -fsS --get \
      --data-urlencode "q=g:\"$group\" AND a:\"$artifact\"" \
      --data-urlencode "rows=1" \
      --data-urlencode "wt=json" \
      "https://search.maven.org/solrsearch/select"
  )"

  num_found="$(echo "$response" | jq -r '.response.numFound // 0')"
  latest_version="$(echo "$response" | jq -r '.response.docs[0].latestVersion // ""')"
  timestamp_ms="$(echo "$response" | jq -r '.response.docs[0].timestamp // 0')"

  if [ "$num_found" -gt 0 ]; then
    deployed=true
    last_updated="$(date -u -r "$((timestamp_ms / 1000))" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || echo "")"
    artifact_url="https://central.sonatype.com/artifact/$group/$artifact"
  else
    deployed=false
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
  echo "| Modulo | Badge | GroupId | ArtifactId | Desplegado | Ultima version | Ultima actualizacion (UTC) |"
  echo "|---|---|---|---|---|---|---|"
  jq -r '
    .[] |
    "| \(.module) | ![\(.module)](https://img.shields.io/maven-central/v/\(.groupId)/\(.artifactId)) | \(.groupId) | \(.artifactId) | " +
    (if .deployed then "si" else "no" end) + " | " +
    (if (.latestVersion|length)>0 then .latestVersion else "-" end) + " | " +
    (if (.lastUpdatedUtc|length)>0 then .lastUpdatedUtc else "-" end) + " |"
  ' "$JSON_FILE"
} > "$TABLE_FILE"

echo "Generated:"
echo "  - $JSON_FILE"
echo "  - $TABLE_FILE"

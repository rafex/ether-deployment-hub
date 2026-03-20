#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

PLAN_PATH="${1:-$ROOT_DIR/release-artifacts/release-plan.json}"
REPORT_PATH="${2:-$ROOT_DIR/release-artifacts/version-collision-report.json}"
REPO_URL="${MAVEN_CENTRAL_REPO_URL:-https://repo1.maven.org/maven2}"
# Set ALLOW_EXISTING=true to treat already_published as a warning, not a collision.
# This allows re-deployments to skip already-published modules gracefully.
ALLOW_EXISTING="${ALLOW_EXISTING:-false}"

if [ ! -f "$PLAN_PATH" ]; then
  echo "Release plan not found: $PLAN_PATH" >&2
  exit 1
fi

mkdir -p "$(dirname "$REPORT_PATH")"

http_code_with_retry() {
  local url="$1"
  local code=""
  local attempt=1
  while [ "$attempt" -le 3 ]; do
    code="$(curl -L -sS --connect-timeout 10 --max-time 30 -o /dev/null -w '%{http_code}' "$url" || true)"
    case "$code" in
      200|404)
        printf '%s\n' "$code"
        return 0
        ;;
      *)
        sleep $((attempt * 2))
        ;;
    esac
    attempt=$((attempt + 1))
  done
  printf '%s\n' "${code:-000}"
  return 0
}

tmp_results="$(mktemp)"
trap 'rm -f "$tmp_results"' EXIT
: > "$tmp_results"

while IFS= read -r module_json; do
  level="$(printf '%s' "$module_json" | jq -r '.releaseLevel')"
  if [ "$level" = "none" ]; then
    continue
  fi

  name="$(printf '%s' "$module_json" | jq -r '.name')"
  group_id="$(printf '%s' "$module_json" | jq -r '.groupId')"
  artifact_id="$(printf '%s' "$module_json" | jq -r '.artifactId')"
  version="$(printf '%s' "$module_json" | jq -r '.nextVersion')"
  group_path="${group_id//./\/}"
  pom_url="${REPO_URL%/}/${group_path}/${artifact_id}/${version}/${artifact_id}-${version}.pom"
  sonatype_url="https://central.sonatype.com/artifact/${group_id}/${artifact_id}"
  code="$(http_code_with_retry "$pom_url")"

  status="unknown"
  collision=false
  case "$code" in
    200)
      status="already_published"
      if [ "$ALLOW_EXISTING" = "true" ]; then
        collision=false
      else
        collision=true
      fi
      ;;
    404)
      status="available"
      collision=false
      ;;
    *)
      status="lookup_error"
      collision=true
      ;;
  esac

  jq -n \
    --arg name "$name" \
    --arg groupId "$group_id" \
    --arg artifactId "$artifact_id" \
    --arg version "$version" \
    --arg pomUrl "$pom_url" \
    --arg sonatypeUrl "$sonatype_url" \
    --arg httpCode "$code" \
    --arg status "$status" \
    --argjson collision "$collision" \
    '{
      name: $name,
      groupId: $groupId,
      artifactId: $artifactId,
      version: $version,
      pomUrl: $pomUrl,
      sonatypeUrl: $sonatypeUrl,
      httpCode: $httpCode,
      status: $status,
      collision: $collision
    }' >> "$tmp_results"
done < <(jq -c '
  .modules[]
  | {name, groupId, artifactId, nextVersion, releaseLevel}
' "$PLAN_PATH")

results_json="$(jq -s '.' "$tmp_results")"
report_json="$(jq -n \
  --arg planPath "$PLAN_PATH" \
  --arg checkedAt "$(date -u +"%Y-%m-%dT%H:%M:%SZ")" \
  --arg repoUrl "$REPO_URL" \
  --argjson modules "$results_json" \
  '{
    planPath: $planPath,
    checkedAtUtc: $checkedAt,
    repoUrl: $repoUrl,
    modules: $modules
  }')"

printf '%s\n' "$report_json" > "$REPORT_PATH"

collisions_count="$(printf '%s' "$report_json" | jq '[.modules[] | select(.collision == true)] | length')"
checked_count="$(printf '%s' "$report_json" | jq '.modules | length')"

echo "Validated planned versions against Maven Central:"
echo "  - checked modules: $checked_count"
echo "  - collisions/errors: $collisions_count"
echo "  - report: $REPORT_PATH"

if [ "$collisions_count" -gt 0 ]; then
  echo "Version collisions detected:"
  printf '%s' "$report_json" | jq -r '
    .modules[]
    | select(.collision == true)
    | "- \(.name): \(.groupId):\(.artifactId):\(.version) [\(.status), http=\(.httpCode)]\n  \(.pomUrl)\n  \(.sonatypeUrl)"
  '
  exit 1
fi

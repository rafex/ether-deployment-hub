#!/usr/bin/env bash
set -euo pipefail

REPO_URL="${1:?Usage: $0 <repo-url> <group-id> <artifact-id> <version> [attempts] [sleep-seconds]}"
GROUP_ID="${2:?Usage: $0 <repo-url> <group-id> <artifact-id> <version> [attempts] [sleep-seconds]}"
ARTIFACT_ID="${3:?Usage: $0 <repo-url> <group-id> <artifact-id> <version> [attempts] [sleep-seconds]}"
VERSION="${4:?Usage: $0 <repo-url> <group-id> <artifact-id> <version> [attempts] [sleep-seconds]}"
ATTEMPTS="${5:-60}"
SLEEP_SECONDS="${6:-30}"

group_path="$(printf '%s' "$GROUP_ID" | tr '.' '/')"
base_url="${REPO_URL%/}/${group_path}/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}"

for attempt in $(seq 1 "$ATTEMPTS"); do
  if curl -fsSL --max-time 15 "${base_url}.pom.sha1" >/dev/null 2>&1 || \
     curl -fsSL --max-time 15 "${base_url}.jar.sha1" >/dev/null 2>&1; then
    echo "Found ${GROUP_ID}:${ARTIFACT_ID}:${VERSION} on Maven Central"
    exit 0
  fi

  if [ "$attempt" -lt "$ATTEMPTS" ]; then
    echo "Waiting for ${GROUP_ID}:${ARTIFACT_ID}:${VERSION} on Maven Central (${attempt}/${ATTEMPTS})..."
    sleep "$SLEEP_SECONDS"
  fi
done

echo "Timed out waiting for ${GROUP_ID}:${ARTIFACT_ID}:${VERSION} on Maven Central" >&2
exit 1

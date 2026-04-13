#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 3 ]; then
  echo "Usage: $0 <repo-base-url> <group-id> <artifact-id>" >&2
  exit 1
fi

repo_base_url="$1"
group_id="$2"
artifact_id="$3"
group_path="$(printf '%s' "$group_id" | tr '.' '/')"
metadata_url="${repo_base_url%/}/${group_path}/${artifact_id}/maven-metadata.xml"

metadata="$(curl -fsSL "$metadata_url")"
latest="$(
  printf '%s' "$metadata" | tr -d '\n' | sed -n 's:.*<release>\([^<]*\)</release>.*:\1:p'
)"

if [ -z "$latest" ]; then
  latest="$(
    printf '%s' "$metadata" | tr -d '\n' | sed -n 's:.*<latest>\([^<]*\)</latest>.*:\1:p'
  )"
fi

if [ -z "$latest" ]; then
  latest="$(
    printf '%s\n' "$metadata" | sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' | tail -n 1
  )"
fi

if [ -z "$latest" ]; then
  echo "Unable to determine latest version from ${metadata_url}" >&2
  exit 1
fi

printf '%s\n' "$latest"

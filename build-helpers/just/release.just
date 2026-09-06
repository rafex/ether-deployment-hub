# Release operations facade (Justfile module).
# No inline logic: recipes compose existing Make targets and scripts.

LOG_FILE := "/tmp/ether-deployment-hub/release-" + `date +%Y%m%d-%H%M%S` + ".log"

[private]
log-dir:
    @mkdir -p $(dirname {{LOG_FILE}})

# env maven: decrypt and export Maven secrets for `eval "$(just env maven)"`.
env maven: log-dir
    @scripts/secrets.sh --action env --show-values --log-file {{LOG_FILE}}

# publish-central [base_ref] [head_ref]: plan, validate and deploy to Maven Central.
publish-central base_ref="" head_ref="": log-dir
    make release-build BASE_REF={{base_ref}} HEAD_REF={{head_ref}}
    scripts/deploy-release.sh --base-ref {{base_ref}} --head-ref {{head_ref}} --log-file {{LOG_FILE}}

# verify-published: wait until every planned artifact is available on Maven Central.
verify-published: log-dir
    #!/usr/bin/env bash
    set -euo pipefail
    plan="release-artifacts/release-plan.json"
    if [ ! -f "$plan" ]; then
        echo "Release plan not found: $plan" >&2
        exit 1
    fi
    repo_url="${MAVEN_CENTRAL_REPO_URL:-https://repo1.maven.org/maven2}"
    jq -c '.modules[] | select(.releaseLevel != "none")' "$plan" | while IFS= read -r mod; do
        group_id=$(printf '%s' "$mod" | jq -r '.groupId')
        version=$(printf '%s' "$mod" | jq -r '.nextVersion')
        artifact_ids=$(printf '%s' "$mod" | jq -r '([.artifactId] + (.subArtifacts // [])) | .[]')
        while IFS= read -r artifact_id; do
            [ -n "$artifact_id" ] || continue
            echo "Verifying ${group_id}:${artifact_id}:${version} ..." >> {{LOG_FILE}}
            scripts/wait-for-maven-central-artifact.sh "$repo_url" "$group_id" "$artifact_id" "$version" >> {{LOG_FILE}} 2>&1
        done <<< "$artifact_ids"
    done

# release: full local Maven Central release (env + publish + verify).
release:
    eval "$(just env maven)"
    just publish-central
    just verify-published

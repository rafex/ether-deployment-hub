#!/usr/bin/env bash
#
# deploy-release.sh — Local release orchestrator for Ether Deployment Hub.
#
# Ports the orchestration logic from .github/workflows/deploy-one-level.yml and
# publish-java-modules-maven-central.yml to a single local script. Reuses the
# existing helper scripts in scripts/; it does not reimplement their logic.
#
# Usage:
#   ./scripts/deploy-release.sh --log-file /var/log/ether-deployment-hub/deploy.log \
#       [--base-ref REF] [--head-ref REF] [--dry-run] [--levels 0,2] \
#       [--skip-tests] [--log-level debug] [--skip-sync-manifest]
#
# Environment (set externally, e.g. via `just env maven`):
#   OSSRH_USERNAME          required for actual deploy
#   OSSRH_PASSWORD          required for actual deploy
#   MAVEN_GPG_PASSPHRASE    consumed by `make deploy` when signing
#   GPG_PRIVATE_KEY_B64     optional; if missing, assumes local keyring has the key

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
RELEASE_ARTIFACTS_DIR="$ROOT_DIR/release-artifacts"
RESULTS_DIR="$RELEASE_ARTIFACTS_DIR/deploy-results"
SCRIPTS_DIR="$ROOT_DIR/scripts"
MANIFEST_PATH="$ROOT_DIR/releases/manifest.json"
REPO_URL="${MAVEN_CENTRAL_REPO_URL:-https://repo1.maven.org/maven2}"

# Defaults
BASE_REF=""
HEAD_REF=""
DRY_RUN="false"
LEVELS=""
SKIP_TESTS="false"
LOG_FILE=""
LOG_LEVEL="info"
SKIP_SYNC_MANIFEST="false"

usage() {
  cat <<EOF
Usage: $(basename "$0") --log-file <path> [options]

Required:
  --log-file <path>            Audit log file path

Options:
  --base-ref <ref>             Base git ref (default: HEAD^, or first commit)
  --head-ref <ref>             Head git ref (default: HEAD)
  --dry-run                    Only sync manifest, generate plan and validate it
  --levels <csv>               Deploy only the listed level indices (e.g. "0,2")
  --skip-tests                 Pass SKIP_TESTS=true to module deploys
  --log-level <info|debug>     Log verbosity (default: info)
  --skip-sync-manifest         Skip sync-manifest-from-central.sh at start
EOF
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --base-ref)
      BASE_REF="$2"
      shift 2
      ;;
    --head-ref)
      HEAD_REF="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN="true"
      shift
      ;;
    --levels)
      LEVELS="$2"
      shift 2
      ;;
    --skip-tests)
      SKIP_TESTS="true"
      shift
      ;;
    --log-file)
      LOG_FILE="$2"
      shift 2
      ;;
    --log-level)
      LOG_LEVEL="$2"
      shift 2
      ;;
    --skip-sync-manifest)
      SKIP_SYNC_MANIFEST="true"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [ -z "$LOG_FILE" ]; then
  echo "ERROR: --log-file is required" >&2
  usage >&2
  exit 1
fi

mkdir -p "$(dirname "$LOG_FILE")" "$RELEASE_ARTIFACTS_DIR" "$RESULTS_DIR"
: > "$LOG_FILE"

log_info() {
  local line
  line="[$(date -u +"%Y-%m-%dT%H:%M:%SZ")] $*"
  echo "$line" | tee -a "$LOG_FILE"
}

log_debug() {
  if [ "$LOG_LEVEL" = "debug" ]; then
    log_info "[DEBUG] $*"
  fi
}

resolve_ref_defaults() {
  if [ -z "$BASE_REF" ]; then
    if git -C "$ROOT_DIR" rev-parse --verify HEAD^ >/dev/null 2>&1; then
      BASE_REF="$(git -C "$ROOT_DIR" rev-parse HEAD^)"
    else
      BASE_REF="$(git -C "$ROOT_DIR" rev-list --max-parents=0 HEAD | tail -n 1)"
    fi
  fi
  if [ -z "$HEAD_REF" ]; then
    HEAD_REF="HEAD"
  fi
}

resolve_ref_defaults

log_info "Starting local release deploy"
log_info "Root directory: $ROOT_DIR"
log_info "Base ref: $BASE_REF"
log_info "Head ref: $HEAD_REF"
log_info "Dry run: $DRY_RUN"
[ -n "$LEVELS" ] && log_info "Selected levels: $LEVELS"
log_info "Skip tests: $SKIP_TESTS"
log_info "Log level: $LOG_LEVEL"
log_info "Audit log: $LOG_FILE"

# Validate required credentials when a real deploy is requested.
if [ "$DRY_RUN" = "false" ]; then
  if [ -z "${OSSRH_USERNAME:-}" ] || [ -z "${OSSRH_PASSWORD:-}" ]; then
    log_info "ERROR: OSSRH_USERNAME and OSSRH_PASSWORD are required for deploy"
    exit 1
  fi

  # Import GPG key only when provided. If not provided, assume the key is
  # already present in the local keyring.
  if [ -n "${GPG_PRIVATE_KEY_B64:-}" ]; then
    log_info "Importing GPG private key..."
    echo "$GPG_PRIVATE_KEY_B64" | base64 --decode | gpg --batch --import
  fi
fi

# Run a helper script, capturing its output to the audit log. In debug mode
# (or dry-run) also stream output to stdout so the operator can see progress.
run_script() {
  local script="$1"
  shift
  local status=0

  log_debug "Running: $script $*"
  if [ "$LOG_LEVEL" = "debug" ] || [ "$DRY_RUN" = "true" ]; then
    "$SCRIPTS_DIR/$script" "$@" 2>&1 | tee -a "$LOG_FILE"
    status=${PIPESTATUS[0]}
  else
    "$SCRIPTS_DIR/$script" "$@" >> "$LOG_FILE" 2>&1
    status=$?
  fi
  return "$status"
}

# 1. Optionally synchronize the manifest with Maven Central.
if [ "$DRY_RUN" = "false" ] && [ "$SKIP_SYNC_MANIFEST" = "false" ]; then
  log_info "Synchronizing manifest from Maven Central..."
  run_script sync-manifest-from-central.sh "$MANIFEST_PATH" "$REPO_URL"
fi

# 2. Generate the release plan.
log_info "Generating release plan..."
run_script generate-release-plan.sh "$BASE_REF" "$HEAD_REF"
PLAN_PATH="$RELEASE_ARTIFACTS_DIR/release-plan.json"

selected_count="$(jq -r '.selectedCount' "$PLAN_PATH")"
log_info "Release plan selected $selected_count module(s)"
log_info "Deploy order: $(jq -r '.deployOrder | join(", ")' "$PLAN_PATH")"

# 3. Validate planned versions against Maven Central.
#    Dry-run reports real collisions (ALLOW_EXISTING=false). Real deploy allows
#    already-published artifacts so the orchestrator can re-run and skip them.
VALIDATION_REPORT="$RELEASE_ARTIFACTS_DIR/version-collision-report.json"
if [ "$DRY_RUN" = "true" ]; then
  log_info "Validating release plan against Maven Central (dry-run)..."
  ALLOW_EXISTING="false" run_script validate-release-plan-against-central.sh "$PLAN_PATH" "$VALIDATION_REPORT"
else
  log_info "Re-validating release plan against Maven Central (allow existing)..."
  ALLOW_EXISTING="true" run_script validate-release-plan-against-central.sh "$PLAN_PATH" "$VALIDATION_REPORT"
fi

if [ "$DRY_RUN" = "true" ]; then
  log_info "Dry run complete. Release plan and validation report generated."
  log_info "  Plan: $PLAN_PATH"
  log_info "  Validation report: $VALIDATION_REPORT"
  exit 0
fi

# 4. Compute topological deploy levels.
LEVELS_JSON_FILE="$RELEASE_ARTIFACTS_DIR/deploy-levels.json"
log_info "Computing deploy levels..."
"$SCRIPTS_DIR/compute-deploy-levels.sh" "$PLAN_PATH" > "$LEVELS_JSON_FILE"
log_debug "Deploy levels: $(cat "$LEVELS_JSON_FILE")"

# 5. Apply release plan versions to POM files.
log_info "Applying release plan versions to POM files..."
run_script apply-release-plan.sh "$PLAN_PATH" "$MANIFEST_PATH"

# Remove stale partial results from previous runs.
rm -f "$RESULTS_DIR"/deploy-results-level-*.json "$RESULTS_DIR"/deploy-*.log

# 6. Deploy modules level by level.
#
# In a local run the Maven local repository (~/.m2) is shared, so modules from
# earlier levels are immediately available as dependencies for later levels.
# Unlike the CI workflow, there is no need to collect/upload artifacts between
# levels.

deployed_json="[]"

add_deployed_entry() {
  local entry="$1"
  local name="${entry% (*}"
  local version="${entry##*(}"
  version="${version%)}"
  deployed_json="$(jq -n \
    --argjson arr "$deployed_json" \
    --arg name "$name" \
    --arg version "$version" \
    '$arr + [{name: $name, version: $version}]')"
}

overall_published=0
overall_skipped=0
overall_failed=0
global_failed="false"
failed_modules_list=()

level_count="$(jq '.levels | length' "$LEVELS_JSON_FILE")"
for ((level_index = 0; level_index < level_count; level_index++)); do
  # Filter levels if --levels was supplied.
  if [ -n "$LEVELS" ]; then
    if ! printf ',%s,' "$LEVELS" | grep -q ",${level_index},"; then
      log_info "Skipping level $level_index (not in --levels $LEVELS)"
      continue
    fi
  fi

  level_json="$(jq -c ".levels[$level_index]" "$LEVELS_JSON_FILE")"
  mapfile -t level_modules < <(printf '%s' "$level_json" | jq -r '.[]')

  if [ "${#level_modules[@]}" -eq 0 ]; then
    log_info "Level $level_index is empty, skipping"
    continue
  fi

  log_info "=========================================================="
  log_info "Level $level_index — ${#level_modules[@]} module(s)"
  log_info "=========================================================="

  ok_modules=()
  skip_modules=()
  fail_modules=()
  level_failed="false"

  for module_name in "${level_modules[@]}"; do
    release_level="$(jq -r --arg m "$module_name" '.modules[] | select(.name == $m) | .releaseLevel' "$PLAN_PATH")"
    if [ "$release_level" = "none" ]; then
      log_info "  [SKIP] $module_name (releaseLevel=none)"
      continue
    fi

    module_path="$(jq -r --arg m "$module_name" '.modules[] | select(.name == $m) | .path' "$PLAN_PATH")"
    next_version="$(jq -r --arg m "$module_name" '.modules[] | select(.name == $m) | .nextVersion' "$PLAN_PATH")"
    group_id="$(jq -r --arg m "$module_name" '.modules[] | select(.name == $m) | .groupId' "$PLAN_PATH")"
    artifact_id="$(jq -r --arg m "$module_name" '.modules[] | select(.name == $m) | .artifactId' "$PLAN_PATH")"

    module_log="$RESULTS_DIR/deploy-${module_name}.log"
    : > "$module_log"

    log_info "  [START] $module_name ($next_version)"

    if (
      cd "$ROOT_DIR/$module_path"
      make deploy \
        VERSION="$next_version" \
        FORCE_EXACT_VERSION=true \
        SYNC_FROM_REPO=false \
        SKIP_TESTS="$SKIP_TESTS" \
        AUTO_UPDATE_LICENSE_HEADERS=true \
        CENTRAL_WAIT_UNTIL=validated
    ) > "$module_log" 2>&1; then
      log_info "  [OK]    $module_name ($next_version)"
      ok_modules+=("$module_name ($next_version)")
      overall_published=$((overall_published + 1))
    else
      if grep -Eq "already exists|Component with package url:.*already exists" "$module_log" 2>/dev/null; then
        log_info "  [SKIP]  $module_name ($next_version) — already published in Maven Central"
        skip_modules+=("$module_name ($next_version)")
        overall_skipped=$((overall_skipped + 1))
      elif grep -Eq "currently being published in another deployment" "$module_log" 2>/dev/null; then
        log_info "  [WAIT]  $module_name ($next_version) — another deployment is publishing it"
        if "$SCRIPTS_DIR/wait-for-maven-central-artifact.sh" "$REPO_URL" "$group_id" "$artifact_id" "$next_version" >> "$LOG_FILE" 2>&1; then
          log_info "  [SKIP]  $module_name ($next_version) — published by in-flight deployment"
          skip_modules+=("$module_name ($next_version)")
          overall_skipped=$((overall_skipped + 1))
        else
          log_info "  [FAIL]  $module_name ($next_version) — not found after waiting"
          {
            echo "---- $module_log ----"
            cat "$module_log"
            echo "----------------------"
          } >> "$LOG_FILE"
          fail_modules+=("$module_name ($next_version)")
          failed_modules_list+=("$module_name ($next_version)")
          overall_failed=$((overall_failed + 1))
          level_failed="true"
        fi
      else
        log_info "  [FAIL]  $module_name ($next_version)"
        {
          echo "---- $module_log ----"
          cat "$module_log"
          echo "----------------------"
        } >> "$LOG_FILE"
        fail_modules+=("$module_name ($next_version)")
        failed_modules_list+=("$module_name ($next_version)")
        overall_failed=$((overall_failed + 1))
        level_failed="true"
      fi
    fi
  done

  # Persist results for this level, mirroring the CI workflow format.
  deployed_json="[]"
  if [ "${#ok_modules[@]}" -gt 0 ]; then
    for entry in "${ok_modules[@]}"; do add_deployed_entry "$entry"; done
  fi
  if [ "${#skip_modules[@]}" -gt 0 ]; then
    for entry in "${skip_modules[@]}"; do add_deployed_entry "$entry"; done
  fi
  jq -n \
    --argjson level "$level_index" \
    --argjson deployed "$deployed_json" \
    '{level: $level, deployed: $deployed}' \
    > "$RESULTS_DIR/deploy-results-level-${level_index}.json"

  log_info "Level $level_index summary — published: ${#ok_modules[@]}, skipped: ${#skip_modules[@]}, failed: ${#fail_modules[@]}"

  if [ "$level_failed" = "true" ]; then
    log_info "ERROR: Level $level_index had failures; stopping further levels."
    global_failed="true"
    break
  fi
done

# 7. Update the manifest from full or partial results.
if [ "$global_failed" = "false" ]; then
  log_info "All levels completed successfully. Updating manifest from plan..."
  run_script update-manifest-from-plan.sh "$PLAN_PATH" "$MANIFEST_PATH"
else
  log_info "Partial failure detected. Updating manifest from deployed results..."
  run_script update-manifest-partial.sh "$MANIFEST_PATH" "$RESULTS_DIR"
fi

# 8. Final summary.
log_info "=========================================================="
log_info "Deploy release summary"
log_info "=========================================================="
log_info "Published: $overall_published"
log_info "Skipped:   $overall_skipped"
log_info "Failed:    $overall_failed"

if [ "$global_failed" = "true" ]; then
  log_info "Failed modules:"
  for entry in "${failed_modules_list[@]}"; do
    log_info "  - $entry"
  done
  log_info "Deploy release finished with failures."
  exit 1
fi

log_info "Deploy release finished successfully."
exit 0

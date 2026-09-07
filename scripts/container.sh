#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PROJECT_NAME="ether-deployment-hub"
CI_IMAGE="ghcr.io/rafex/ether-deployment-hub/ci:latest"
CONTAINERFILE="containers/ci/Containerfile"
WORKSPACE="$REPO_ROOT"
CONTAINER_RUNTIME=""
LOG_FILE=""
LOG_LEVEL="info"

action=""
cmd_args=()

usage() {
  cat >&2 <<EOF
Usage: $(basename "$0") --action <runtime|image|image-pull|ci|run> --log-file <path> [options] [-- <command>]

Actions:
  runtime     Print the detected container runtime (podman or docker).
  image       Build the CI image from the Containerfile.
  image-pull  Pull the CI image from the registry.
  ci          Build the image if missing, then run "make validate-main-build" inside it.
  run         Run an arbitrary command inside the container. Must be followed by -- <command>.

Options:
  --project-name <name>     Project name for log directory fallback. Default: ${PROJECT_NAME}
  --container-runtime <r>   Force podman or docker.
  --ci-image <image>        CI image reference. Default: ${CI_IMAGE}
  --containerfile <path>    Path to the Containerfile. Default: ${CONTAINERFILE}
  --workspace <path>        Host workspace mounted at /workspace. Default: ${WORKSPACE}
  --log-file <path>         Required. Audit log destination.
  --log-level <level>       Optional. Default: ${LOG_LEVEL}
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --)
      shift
      while [[ $# -gt 0 ]]; do
        cmd_args+=("$1")
        shift
      done
      break
      ;;
    --action)
      action="${2:-}"; shift 2 ;;
    --project-name)
      PROJECT_NAME="${2:-$PROJECT_NAME}"; shift 2 ;;
    --container-runtime)
      CONTAINER_RUNTIME="${2:-}"; shift 2 ;;
    --ci-image)
      CI_IMAGE="${2:-$CI_IMAGE}"; shift 2 ;;
    --containerfile)
      CONTAINERFILE="${2:-$CONTAINERFILE}"; shift 2 ;;
    --workspace)
      WORKSPACE="${2:-$WORKSPACE}"; shift 2 ;;
    --log-file)
      LOG_FILE="${2:-}"; shift 2 ;;
    --log-level)
      LOG_LEVEL="${2:-info}"; shift 2 ;;
    -h|--help)
      usage; exit 0 ;;
    *)
      echo "Unknown flag: $1" >&2
      usage
      exit 1 ;;
  esac
done

if [[ -z "$action" ]]; then
  echo "Missing required flag --action" >&2
  usage
  exit 1
fi

if [[ -z "$LOG_FILE" ]]; then
  echo "Missing required flag --log-file" >&2
  usage
  exit 1
fi

log_dir="$(dirname "$LOG_FILE")"
if [[ ! -d "$log_dir" ]]; then
  if ! mkdir -p "$log_dir" 2>/dev/null; then
    fallback_dir="/tmp/${PROJECT_NAME}"
    echo "Cannot create log directory: $log_dir; falling back to ${fallback_dir}" >&2
    mkdir -p "$fallback_dir"
    LOG_FILE="${fallback_dir}/container.log"
  fi
fi

# Redirect only stderr to the audit log so stdout remains clean for consumers.
exec 2>>"$LOG_FILE"

log() {
  local level="$1"
  shift
  echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] [$level] $*" >&2
}

log_info()  { log "INFO" "$@"; }
log_warn()  { log "WARN" "$@"; }
log_debug() { if [[ "$LOG_LEVEL" == "debug" ]]; then log "DEBUG" "$@"; fi; }
log_error() { log "ERROR" "$@"; }

log_info "Audit log: $LOG_FILE"
log_info "Action: $action"
log_info "Repository root: $REPO_ROOT"
log_info "Workspace: $WORKSPACE"
log_info "CI image: $CI_IMAGE"
log_info "Containerfile: $CONTAINERFILE"

resolve_runtime() {
  if [[ -n "$CONTAINER_RUNTIME" ]]; then
    if ! command -v "$CONTAINER_RUNTIME" >/dev/null 2>&1; then
      log_error "Requested container runtime not found: $CONTAINER_RUNTIME"
      exit 1
    fi
    printf '%s\n' "$CONTAINER_RUNTIME"
    return 0
  fi

  if command -v podman >/dev/null 2>&1; then
    printf '%s\n' "podman"
    return 0
  fi

  if command -v docker >/dev/null 2>&1; then
    log_warn "Docker detectado. Podman es la opcion recomendada segun la regla 08-stack."
    printf '%s\n' "docker"
    return 0
  fi

  log_error "No container runtime found. Install podman (recommended) or docker and ensure it is in PATH."
  exit 1
}

ensure_host_m2() {
  local host_m2="${HOME}/.m2"
  if [[ ! -d "$host_m2" ]]; then
    mkdir -p "$host_m2"
    log_info "Created host Maven cache directory: $host_m2"
  fi
}

do_runtime() {
  local runtime
  runtime="$(resolve_runtime)"
  log_info "Runtime: $runtime"
  printf '%s\n' "$runtime"
}

do_image() {
  local runtime
  runtime="$(resolve_runtime)"
  log_info "Building CI image with $runtime: $CI_IMAGE"
  "$runtime" build -f "$CONTAINERFILE" -t "$CI_IMAGE" "$WORKSPACE"
}

do_image_pull() {
  local runtime
  runtime="$(resolve_runtime)"
  log_info "Pulling CI image with $runtime: $CI_IMAGE"
  "$runtime" pull "$CI_IMAGE"
}

do_run() {
  if [[ ${#cmd_args[@]} -eq 0 ]]; then
    log_error "Action 'run' requires a command after --"
    usage
    exit 1
  fi

  local runtime
  runtime="$(resolve_runtime)"

  ensure_host_m2

  local uid_val gid_val
  uid_val="$(id -u)"
  gid_val="$(id -g)"

  local container_workspace="/workspace"
  local container_home="/var/maven"
  local container_gnupghome="${container_home}/.gnupg"

  local -a run_args=(
    "$runtime" "run" "--rm"
    "-u" "${uid_val}:${gid_val}"
    "-v" "${WORKSPACE}:${container_workspace}"
    "-w" "${container_workspace}"
    "-e" "HOME=${container_home}"
    "-v" "${HOME}/.m2:${container_home}/.m2"
    "-e" "GNUPGHOME=${container_gnupghome}"
  )

  if [[ -n "${OSSRH_USERNAME:-}" ]]; then run_args+=("-e" "OSSRH_USERNAME"); fi
  if [[ -n "${OSSRH_PASSWORD:-}" ]]; then run_args+=("-e" "OSSRH_PASSWORD"); fi
  if [[ -n "${MAVEN_GPG_PASSPHRASE:-}" ]]; then run_args+=("-e" "MAVEN_GPG_PASSPHRASE"); fi
  if [[ -n "${GPG_PRIVATE_KEY_B64:-}" ]]; then run_args+=("-e" "GPG_PRIVATE_KEY_B64"); fi

  local cmd_str
  cmd_str="$(printf '%q ' "${cmd_args[@]}")"
  cmd_str="${cmd_str% }"

  log_info "Running command in container: ${cmd_args[*]}"

  run_args+=("$CI_IMAGE" "bash" "-lc" "$cmd_str")
  "${run_args[@]}"
}

do_ci() {
  local runtime
  runtime="$(resolve_runtime)"

  if ! "$runtime" image inspect "$CI_IMAGE" >/dev/null 2>&1; then
    log_info "CI image not found locally; building $CI_IMAGE"
    do_image
  else
    log_debug "CI image found locally: $CI_IMAGE"
  fi

  cmd_args=("make" "validate-main-build")
  do_run
}

case "$action" in
  runtime)
    do_runtime
    ;;
  image)
    do_image
    ;;
  image-pull)
    do_image_pull
    ;;
  ci)
    do_ci
    ;;
  run)
    do_run
    ;;
  *)
    echo "Unknown action: $action. Expected runtime|image|image-pull|ci|run" >&2
    usage
    exit 1
    ;;
esac

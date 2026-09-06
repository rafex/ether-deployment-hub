#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

PROJECT_NAME="ether-deployment-hub"
AGE_KEY_DIR="${HOME}/.age"
AGE_KEY_FILE="${AGE_KEY_DIR}/${PROJECT_NAME}-key.txt"
SECRETS_DIR="${REPO_ROOT}/.secrets"
SECRETS_FILE="${SECRETS_DIR}/secrets.maven.enc.yaml"

REQUIRED_KEYS=(
  "OSSRH_USERNAME"
  "OSSRH_PASSWORD"
  "MAVEN_GPG_PASSPHRASE"
  "GPG_PRIVATE_KEY_B64"
)

action=""
log_file=""
log_level="info"
show_values="false"

usage() {
  cat >&2 <<EOF
Usage: $(basename "$0") --action <edit|env|verify|keygen> --log-file <path> [--log-level info|debug] [--show-values]

Commands:
  edit    Open secrets.maven.enc.yaml in \$EDITOR for modification.
  env     Decrypt secrets and, with --show-values, emit export statements for eval.
  verify  Decrypt secrets and validate that required keys are present.
  keygen  Generate an age key at ~/.age/ether-deployment-hub-key.txt if absent.

Options:
  --log-file <path>   Required. Audit log destination.
  --log-level <level> Optional. Default: info.
  --show-values       For 'env': emit 'export KEY=value' to stdout for shell eval.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --action)
      action="${2:-}"; shift 2 ;;
    --log-file)
      log_file="${2:-}"; shift 2 ;;
    --log-level)
      log_level="${2:-info}"; shift 2 ;;
    --show-values)
      show_values="true"; shift ;;
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

if [[ -z "$log_file" ]]; then
  echo "Missing required flag --log-file" >&2
  usage
  exit 1
fi

log_dir="$(dirname "$log_file")"
if [[ ! -d "$log_dir" ]]; then
  mkdir -p "$log_dir" || {
    echo "Cannot create log directory: $log_dir" >&2
    exit 1
  }
fi

# Redirect only stderr to the audit log so stdout remains clean for shell eval.
exec 2>>"$log_file"

log() {
  local level="$1"
  shift
  echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] [$level] $*" >&2
}

log_info()  { log "INFO" "$@"; }
log_debug() { if [[ "$log_level" == "debug" ]]; then log "DEBUG" "$@"; fi; }
log_error() { log "ERROR" "$@"; }

log_info "Audit log: $log_file"
log_info "Action: $action"
log_info "Repository root: $REPO_ROOT"

require_sops() {
  if ! command -v sops >/dev/null 2>&1; then
    log_error "sops is not installed. Install it with: brew install sops (macOS) or apt install sops (Linux)"
    exit 1
  fi
}

require_age() {
  if ! command -v age-keygen >/dev/null 2>&1; then
    log_error "age is not installed. Install it with: brew install age (macOS) or apt install age (Linux)"
    exit 1
  fi
}

require_age_key() {
  if [[ ! -f "$AGE_KEY_FILE" ]]; then
    log_error "age private key not found at $AGE_KEY_FILE"
    log_error "Generate one with: $0 --action keygen --log-file <path>"
    exit 1
  fi
  export SOPS_AGE_KEY_FILE="$AGE_KEY_FILE"
}

ensure_secrets_file() {
  if [[ ! -f "$SECRETS_FILE" ]]; then
    log_error "Encrypted secrets file not found: $SECRETS_FILE"
    exit 1
  fi
}

do_keygen() {
  require_age
  mkdir -p "$AGE_KEY_DIR"
  chmod 700 "$AGE_KEY_DIR"

  if [[ -f "$AGE_KEY_FILE" ]]; then
    log_info "age key already exists at $AGE_KEY_FILE; will not overwrite."
    echo "Public key: $(age-keygen -y "$AGE_KEY_FILE")"
    return 0
  fi

  age-keygen -o "$AGE_KEY_FILE"
  chmod 600 "$AGE_KEY_FILE"
  log_info "age key generated at $AGE_KEY_FILE"
  echo "Public key: $(age-keygen -y "$AGE_KEY_FILE")"
  echo "Add the public key above to .sops.yaml under creation_rules -> age."
}

do_edit() {
  require_sops
  require_age
  require_age_key

  mkdir -p "$SECRETS_DIR"
  if [[ ! -f "$SECRETS_FILE" ]]; then
    log_info "Creating new encrypted secrets file: $SECRETS_FILE"
  fi

  log_info "Opening $SECRETS_FILE with editor: ${EDITOR:-default}"
  sops edit "$SECRETS_FILE"
  log_info "$SECRETS_FILE saved and encrypted."
}

do_env() {
  require_sops
  require_age
  require_age_key
  ensure_secrets_file

  log_debug "Decrypting $SECRETS_FILE"

  if [[ "$show_values" != "true" ]]; then
    # Validate decryption without leaking values.
    sops --decrypt "$SECRETS_FILE" >/dev/null
    log_info "Secrets decrypted successfully."
    log_info "To export variables in your current shell, run:"
    log_info "  eval \"\$($0 --action env --log-file $log_file --show-values)\""
    return 0
  fi

  # Emit export statements on stdout for shell eval. These are intentionally
  # not written to the audit log.
  sops --decrypt --output-type json "$SECRETS_FILE" | python3 -c '
import sys, json, shlex
data = json.load(sys.stdin)
for key, value in data.items():
    print(f"export {key}={shlex.quote(str(value))}")
'
}

do_verify() {
  require_sops
  require_age
  require_age_key
  ensure_secrets_file

  log_info "Decrypting and verifying required keys..."

  local decrypted_json
  decrypted_json="$(sops --decrypt --output-type json "$SECRETS_FILE")"

  local missing placeholders
  missing="$(echo "$decrypted_json" | python3 -c '
import sys, json
required = ["OSSRH_USERNAME", "OSSRH_PASSWORD", "MAVEN_GPG_PASSPHRASE", "GPG_PRIVATE_KEY_B64"]
data = json.load(sys.stdin)
missing = [k for k in required if not data.get(k)]
print(" ".join(missing))
')"

  placeholders="$(echo "$decrypted_json" | python3 -c '
import sys, json
required = ["OSSRH_USERNAME", "OSSRH_PASSWORD", "MAVEN_GPG_PASSPHRASE", "GPG_PRIVATE_KEY_B64"]
data = json.load(sys.stdin)
placeholders = [k for k in required if data.get(k) == "CHANGE_ME"]
print(" ".join(placeholders))
')"

  if [[ -n "$missing" ]]; then
    log_error "Missing required keys: $missing"
    exit 1
  fi

  log_info "All required keys are present."

  if [[ -n "$placeholders" ]]; then
    log_info "Warning: the following keys still have placeholder values: $placeholders"
    log_info "Replace them with: $0 --action edit --log-file <path>"
  fi
}

case "$action" in
  keygen)
    do_keygen
    ;;
  edit)
    do_edit
    ;;
  env)
    do_env
    ;;
  verify)
    do_verify
    ;;
  *)
    echo "Unknown action: $action. Expected edit|env|verify|keygen" >&2
    usage
    exit 1
    ;;
esac

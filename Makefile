# Set the directory for this project so make deploy need not receive PROJECT_DIR
PROJECT_DIR         := ether-http-problem
PROJECT_GROUP_ID    := dev.rafex.ether.http
PROJECT_ARTIFACT_ID := ether-http-problem
DEPENDENCY_COORDS   := ether-http-core.version=dev.rafex.ether.http:ether-http-core ether-json.version=dev.rafex.ether.json:ether-json

# Include shared build logic
include ../build-helpers/common.mk
include ../build-helpers/git.mk

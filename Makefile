# Set the directory for this project so make deploy need not receive PROJECT_DIR
PROJECT_DIR         := ether-glowroot-jetty12
PROJECT_GROUP_ID    := dev.rafex.ether.glowroot
PROJECT_ARTIFACT_ID := ether-glowroot-jetty12
DEPENDENCY_COORDS   := ether-http-core.version=dev.rafex.ether.http:ether-http-core ether-http-jetty12.version=dev.rafex.ether.http:ether-http-jetty12 ether-json.version=dev.rafex.ether.json:ether-json ether-websocket-core.version=dev.rafex.ether.websocket:ether-websocket-core ether-websocket-jetty12.version=dev.rafex.ether.websocket:ether-websocket-jetty12

# Include shared build logic
include ../build-helpers/common.mk
include ../build-helpers/git.mk

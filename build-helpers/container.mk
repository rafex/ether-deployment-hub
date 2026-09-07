# Containerized CI/CD facade.
# Delegates all runtime and image operations to scripts/container.sh.
# No inline logic beyond variable defaults and command composition.

CONTAINER_RUNTIME ?=
CI_IMAGE        ?= ghcr.io/rafex/ether-deployment-hub/ci:latest
CONTAINERFILE   ?= containers/ci/Containerfile
CONTAINER_LOG_FILE ?= /tmp/ether-deployment-hub/container-$(shell date +%Y%m%d-%H%M%S).log

# Only pass --container-runtime when the caller explicitly sets one.
CONTAINER_RUNTIME_ARG := $(if $(CONTAINER_RUNTIME),--container-runtime $(CONTAINER_RUNTIME),)

# Common flags shared by every container.sh invocation.
CONTAINER_BASE_FLAGS := \
	--project-name ether-deployment-hub \
	--ci-image $(CI_IMAGE) \
	--containerfile $(CONTAINERFILE) \
	--workspace $(CURDIR) \
	--log-file $(CONTAINER_LOG_FILE) \
	$(CONTAINER_RUNTIME_ARG)

.PHONY: runtime image image-pull ci

## runtime: detect and print the available container runtime (podman or docker)
runtime:
	@mkdir -p "$(dir $(CONTAINER_LOG_FILE))"
	@./scripts/container.sh --action runtime $(CONTAINER_BASE_FLAGS)

## image: build the CI/CD container image from $(CONTAINERFILE)
image:
	@mkdir -p "$(dir $(CONTAINER_LOG_FILE))"
	@./scripts/container.sh --action image $(CONTAINER_BASE_FLAGS)

## image-pull: pull the pre-built CI image from the registry
image-pull:
	@mkdir -p "$(dir $(CONTAINER_LOG_FILE))"
	@./scripts/container.sh --action image-pull $(CONTAINER_BASE_FLAGS)

## ci: run the validation build (make validate-main-build) inside the container
ci:
	@mkdir -p "$(dir $(CONTAINER_LOG_FILE))"
	@./scripts/container.sh --action ci $(CONTAINER_BASE_FLAGS)

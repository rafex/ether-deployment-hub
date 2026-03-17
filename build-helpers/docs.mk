.PHONY: docs-check docs-clean docs-gen docs-ci docs-gen-local docs-gen-docker

DOXYGEN_CONFIG ?= Doxyfile
DOXYGEN_OUT_DIR ?= docs/api/doxygen
DOXYGEN_USE_DOCKER ?= true
DOXYGEN_IMAGE ?= ghcr.io/doxygen/doxygen:latest

## docs-check: verify required tooling for docs generation
docs-check:
ifeq ($(DOXYGEN_USE_DOCKER),true)
	@command -v docker >/dev/null 2>&1 || { echo "Error: docker not found"; exit 1; }
	@echo "docker detected (image: $(DOXYGEN_IMAGE))"
else
	@command -v doxygen >/dev/null 2>&1 || { echo "Error: doxygen not found"; exit 1; }
	@command -v dot >/dev/null 2>&1 || { echo "Error: graphviz (dot) not found"; exit 1; }
	@echo "doxygen + graphviz detected"
endif

## docs-clean: remove generated Doxygen output
docs-clean:
	@rm -rf "$(DOXYGEN_OUT_DIR)"
	@echo "Removed $(DOXYGEN_OUT_DIR)"

## docs-gen: generate Doxygen documentation with Graphviz diagrams
docs-gen: docs-check
ifeq ($(DOXYGEN_USE_DOCKER),true)
	@DOXYGEN_IMAGE="$(DOXYGEN_IMAGE)" ./scripts/doxygenw.sh "$(DOXYGEN_CONFIG)"
else
	@./scripts/generate-doxygen-docs.sh "$(DOXYGEN_CONFIG)"
endif

## docs-gen-local: force local host tooling
docs-gen-local:
	@$(MAKE) docs-gen DOXYGEN_USE_DOCKER=false

## docs-gen-docker: force docker wrapper
docs-gen-docker:
	@$(MAKE) docs-gen DOXYGEN_USE_DOCKER=true

## docs-ci: deterministic docs generation for CI
docs-ci: docs-clean docs-gen
	@echo "Docs generated in $(DOXYGEN_OUT_DIR)"

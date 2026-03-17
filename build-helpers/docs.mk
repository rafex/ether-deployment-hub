.PHONY: docs-check docs-clean docs-gen docs-ci

DOXYGEN_CONFIG ?= Doxyfile
DOXYGEN_OUT_DIR ?= docs/api/doxygen

## docs-check: verify required tooling for docs generation
docs-check:
	@command -v doxygen >/dev/null 2>&1 || { echo "Error: doxygen not found"; exit 1; }
	@command -v dot >/dev/null 2>&1 || { echo "Error: graphviz (dot) not found"; exit 1; }
	@echo "doxygen + graphviz detected"

## docs-clean: remove generated Doxygen output
docs-clean:
	@rm -rf "$(DOXYGEN_OUT_DIR)"
	@echo "Removed $(DOXYGEN_OUT_DIR)"

## docs-gen: generate Doxygen documentation with Graphviz diagrams
docs-gen: docs-check
	@./scripts/generate-doxygen-docs.sh "$(DOXYGEN_CONFIG)"

## docs-ci: deterministic docs generation for CI
docs-ci: docs-clean docs-gen
	@echo "Docs generated in $(DOXYGEN_OUT_DIR)"

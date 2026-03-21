include build-helpers/git.mk
include build-helpers/compile.mk
include build-helpers/gh.mk
include build-helpers/docs.mk

.PHONY: help validate-submodule-refs

## validate-submodule-refs: verify hub submodule SHAs exist in remotes
validate-submodule-refs:
	@./scripts/validate-submodule-remote-refs.sh

## help: show this help message
help:
	@echo "Ether Deployment Hub - Makefile Commands"
	@echo ""
	@echo "Git Operations:"
	@echo "  make new TAG=<version> MESSAGE=\"msg\"    - Create new Git tag"
	@echo "  make retag TAG=<version> MESSAGE=\"msg\"  - Recreate tag (force)"
	@echo ""
	@echo "Submodule Management:"
	@echo "  make submodules-status    - Check status of all submodules"
	@echo "  make submodules-push      - Push submodules with pending commits"
	@echo "  make submodules-update    - Update submodules to latest versions"
	@echo "  make submodules-init      - Initialize and update all submodules"
	@echo "  make submodules-clean     - Reset all submodules (loses changes)"
	@echo "  make submodules-all       - Run complete workflow"
	@echo "  make submodules-pull      - Pull all submodules and update parent"
	@echo "  make validate-submodule-refs - Verify hub submodule SHAs exist in remotes"
	@echo "  make submodules-help      - Detailed submodule help"
	@echo ""
	@echo "Documentation:"
	@echo "  make docs-gen             - Generate documentation"
	@echo "  make docs-gen-docker      - Generate docs using Docker"
	@echo "  make docs-gen-local       - Generate docs locally"
	@echo "  make docs-ci              - CI documentation generation"
	@echo "  make docs-clean           - Clean documentation"
	@echo ""
	@echo "For more details on any command, check the corresponding .mk file"
	@echo "or run: make submodules-help"

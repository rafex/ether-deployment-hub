include build-helpers/git.mk
include build-helpers/compile.mk
include build-helpers/gh.mk
include build-helpers/docs.mk

.PHONY: help validate-source-refs validate-subtree-source-refs

## validate-source-refs: verify module source SHAs exist in remotes
validate-source-refs:
	@if [ -f ./releases/subtrees.json ]; then ./scripts/validate-subtree-source-refs.sh; fi

## validate-subtree-source-refs: verify recorded subtree source SHAs exist in remotes
validate-subtree-source-refs:
	@./scripts/validate-subtree-source-refs.sh

## help: show this help message
help:
	@echo "Ether Deployment Hub - Makefile Commands"
	@echo ""
	@echo "Git Operations:"
	@echo "  make new TAG=<version> MESSAGE=\"msg\"    - Create new Git tag"
	@echo "  make retag TAG=<version> MESSAGE=\"msg\"  - Recreate tag (force)"
	@echo ""
	@echo "Subtree Management:"
	@echo "  make subtrees-status      - Show configured subtrees and source SHAs"
	@echo "  make subtrees-pull        - Pull every subtree from its configured source branch"
	@echo "  make validate-source-refs    - Verify module source SHAs exist in remotes"
	@echo "  make validate-subtree-source-refs - Verify recorded subtree source SHAs exist in remotes"
	@echo "  make subtrees-help        - Detailed subtree help"
	@echo ""
	@echo "Documentation:"
	@echo "  make docs-gen             - Generate documentation"
	@echo "  make docs-gen-docker      - Generate docs using Docker"
	@echo "  make docs-gen-local       - Generate docs locally"
	@echo "  make docs-ci              - CI documentation generation"
	@echo "  make docs-clean           - Clean documentation"
	@echo ""
	@echo "Release & Deploy:"
	@echo "  make sync-manifest        - Sync manifest versions from Maven Central"
	@echo "  make verify-central       - Probe Maven Central to confirm published artifacts exist"
	@echo "  make release-plan         - Generate release plan (BASE_REF/HEAD_REF)"
	@echo "  make validate-release-plan - Check planned versions don't collide in Central"
	@echo "  make deploy               - Full pre-deploy sequence (sync+verify+build+plan)"
	@echo "  make publish-ci           - Trigger GitHub Actions Maven Central publish"
	@echo "  make publish-plan-ci      - Dry-run publish (no actual deploy)"
	@echo ""
	@echo "For more details on any command, check the corresponding .mk file"
	@echo "or run: make subtrees-help"

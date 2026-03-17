# AGENTS

## Deployment automation status
- Release planning is dynamic and module-aware (change detection + semver bumping).
- Publish pipeline validates Maven Central collisions before deploy and re-validates before publish.
- Deploy order is controlled by `releases/manifest.json` (`deployOrder`) and enforced in release planning.
- Deploy is idempotent for already-published artifacts (`already exists` collisions are skipped per module).
- Manifest is synchronized against Maven Central before generating release plans.

## Current deploy order
1. `ether-parent`
2. `ether-json`
3. `ether-jwt`
4. `ether-http-core`
5. `ether-websocket-core`
6. `ether-http-jetty12`
7. `ether-websocket-jetty12`

## CI/runtime conventions
- GitHub workflows force JavaScript actions to Node 24 via:
  - `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24=true`
- Central publishing waits for availability with:
  - `central.waitUntil=published` (default, configurable)

## Documentation stack (phase 1)
- Added Doxygen + Graphviz generation for Java API docs.
- Main config: `Doxyfile` (root).
- Local generation:
  - `make docs-gen`
  - `make docs-ci`
  - `make docs-clean`
- CI workflow:
  - `.github/workflows/generate-doxygen-docs.yml`
  - Publishes `doxygen-html` artifact from `docs/api/doxygen/html`.

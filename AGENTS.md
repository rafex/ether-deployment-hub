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

## Java platform
- **Target JDK: 25 LTS** (Temurin) — migrated from Java 21 on branch `migrate-jdk25`.
- CI runners use `actions/setup-java@v5` with `distribution: temurin`, `java-version: '25'`.
- Maven Wrapper: `3.9.12` (compatible with Java 25).

### GlowRoot strategy (opción B)
`glowroot-agent-api` compile dependency stays at **0.14.0-beta.3** (last version on Maven Central).
The stable release **0.14.5** (with JDK 25 support) is only distributed as a ZIP from GitHub Releases
and is not published to Maven Central. Since `glowroot-agent-api` is a compile-only dependency,
binary compatibility between versions is sufficient for building `ether-glowroot-jetty12`.

For **runtime**: use `glowroot.jar >= 0.14.5` from https://github.com/glowroot/glowroot/releases.

**Important**: JDK 24+ blocks dynamic agent loading by default. Add the following JVM flag when
attaching the GlowRoot agent at runtime:
```
-XX:+EnableDynamicAgentLoading
```

Reference: https://github.com/glowroot/glowroot/releases/tag/v0.14.5

## Publish CI architecture

The publish pipeline is split into two independent workflows:

### 1. `publish-java-modules-maven-central.yml`
- **Trigger**: tag push or `workflow_dispatch` (with `base_ref`, `head_ref`, `run_deploy`).
- **Permissions**: `contents: write`.
- **Jobs**: `release-plan` → `deploy-level-0..6` → `update-manifest`.
- Each `deploy-level-N` job calls `deploy-one-level.yml`, which after publishing also:
  - Collects built JARs/POMs from `~/.m2/repository` into a staging directory.
  - Uploads them as `maven-artifacts-level-N` GitHub Actions artifact.
- Uploads `release-plan` artifact for downstream consumption.
- `make publish-ci` triggers this workflow.

### 2. `publish-java-modules-gh-pkg.yml`
- **Trigger**: `workflow_run` on "Publish Java Modules - Maven Central" with `conclusion == 'success'`,
  or `workflow_dispatch` with `maven_central_run_id` for manual re-runs.
- **Permissions**: `packages: write`, `actions: read`, `contents: read`.
- **Jobs**: `setup` → `deploy-gh-packages-level-0..6`.
- `setup` job downloads `release-plan` artifact from the Maven Central run using `run-id`,
  re-uploads it, and re-computes deployment levels with `compute-deploy-levels.sh`.
- Each `deploy-gh-packages-level-N` job calls `deploy-github-packages-one-level.yml`, which:
  - Downloads `maven-artifacts-level-N` from the Maven Central run (no Maven Central polling).
  - Restores the JARs/POMs to `~/.m2/repository`.
  - Signs and deploys to `https://maven.pkg.github.com/${{ github.repository }}`.
- `make publish-gh-pkg-ci RUN_ID=<maven-central-run-id>` triggers manual re-runs.

### Deployment level chunking
`compute-deploy-levels.sh` uses Kahn's topological sort + `MAX_LEVEL_SIZE=5` chunking to prevent
30-minute GitHub Actions job timeouts. For the full 22-module release the result is 7 levels:
`L0(1) L1(5) L2(4) L3(5) L4(4) L5(2) L6(1)`.

### GPG signing parallelism
`deploy-to-github-packages.sh` uses `MAX_GH_PARALLEL=4` semaphore-based bounded parallelism
to prevent `gpg: signing failed: Cannot allocate memory` on shared GitHub Actions runners.

## Make targets (CI)

| Target | Description |
|---|---|
| `make deploy` | Local install + generate release plan |
| `make publish-ci` | Trigger Maven Central workflow (`run_deploy=true`) |
| `make publish-plan-ci` | Dry-run Maven Central workflow (`run_deploy=false`) |
| `make publish-gh-pkg-ci RUN_ID=<id>` | Manual re-trigger of GH Packages workflow |
| `make gh-runs` | List recent runs for both publish workflows |
| `make gh-watch RUN_ID=<id>` | Watch a specific run |
| `make gh-logs RUN_ID=<id>` | View logs for a specific run |

## Documentation stack (phase 1)
- Added Doxygen + Graphviz generation for Java API docs.
- Main config: `Doxyfile` (root).
- Docker wrapper script: `scripts/doxygenw.sh` (default runner).
- Local generation:
  - `make docs-gen`
  - `make docs-gen-docker`
  - `make docs-gen-local`
  - `make docs-ci`
  - `make docs-clean`
- CI workflow:
  - `.github/workflows/generate-doxygen-docs.yml`
  - Builds docs on PR/push and publishes to GitHub Pages on `main`/manual runs.
  - Upload path: `docs/api/doxygen/html`.

## Java 25 code improvements (branch `migrate-jdk25`)

Applied on 2026-03-22 across 5 submodules:

| Module | File | Improvement | JEP |
|---|---|---|---|
| ether-http-core | `DefaultErrorMapper.java` | `if-instanceof` → `switch` expression | 441 |
| ether-http-problem | `ProblemHttpErrorMapper.java` | `if-instanceof-&&` → `switch` with `when` guard | 441 |
| ether-jdbc | `JdbcDatabaseClient.java` | `wrap()` if-chain → `switch` expression | 441 |
| ether-config | `ReloadableConfigSource.java` | `Thread.ofPlatform()` → `Thread.ofVirtual()` + `catch (X _)` | 444 + 456 |
| ether-jwt | `JwtAlgorithm.java` | for loop → `Arrays.stream().filter().findFirst()` | — |

Pending (preview features, apply when stabilized):
- Structured Concurrency (JEP 505, preview 5) in `ether-ai-openai` / `ether-ai-deepseek`.
- Scoped Values (JEP 506, finalized) to replace ThreadLocal in HTTP request context if added.
- Primitive types in Patterns (JEP 488, preview) in config/json modules.

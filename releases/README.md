# Release Planning

This directory contains the metadata used to calculate module-scoped release plans.

## Files

- `manifest.json`: source of truth for module versions, internal dependencies and release policy.

## Current scope

The release planner currently:

- detects which modules changed between two Git refs
- classifies the change as `none`, `patch`, `minor` or `major`
- propagates dependency-driven releases as `patch`
- generates a release plan and per-module changelog artifacts
- validates target versions against Maven Central before publish
- applies the resulting version plan to module `pom.xml` files
- updates the internal version catalog in `ether-parent`
- is consumed by the GitHub Actions publish workflow to deploy to Maven Central in manifest order

## Notes

- `manifest.json` is the source of truth for module ordering, internal dependencies and release propagation.
- The hub uses `independent` versioning, but the manifest can still be aligned intentionally to start a new major line across many modules.
- Development should stay on `*-SNAPSHOT`; the release plan removes `SNAPSHOT` only for the version being published.

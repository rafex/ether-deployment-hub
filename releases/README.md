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

The planner does not publish to Maven Central yet. It is meant to make release
decisions visible before wiring automated deploys.

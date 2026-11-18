# 0004: Monorepo with multiple services

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

One repository holding all services. Each service in its own top-level folder:

- `api/`: Java API service
- `scan/`: Java scan worker
- `aggregator/`: Java aggregator (stage 6, folder reserved)
- `collector/`: Python upstream collector
- `web/`: Vite + React frontend

`docker-compose.yml` and `nginx/` stay at the repo root.

## Options considered

- Monorepo: one compose file, atomic cross-service changes, shared CI. Larger repo, mixed tooling in one place.
- Multi-repo: independent CI and release, clean boundaries. More overhead for a solo owner, cross-service PRs.

## Not decided here

- Gradle multi-module layout across the Java services. `api/` is currently a standalone Gradle build with its own wrapper. Revisit when `scan/` and `aggregator/` are added (separate builds vs one root build)
- Where the impact matcher and notifier live (open in `architecture.md`; no folders created for them)
- Where shared things go (migrations, vendor registry), if anywhere
- CI layout

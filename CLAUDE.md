# Driftline

API changelog watcher. Watches third-party APIs a team depends on, diffs their specs/changelogs, and alerts when a change affects the customer's actual usage. Product project, not a learning project.

## Docs (read before working)

- `docs/product.md`: problem, users, non-goals, risks
- `docs/architecture.md`: services, data flow, diagram, open items
- `docs/plan.md`: staged plan (stage 0 to 7)

## Stack (decided)

- Java: API, scan service (worker), aggregator, and the rest of the backend. Built with Gradle (`docs/decisions/0001-gradle-java-build-tool.md`). Spring Boot (`docs/decisions/0005-spring-boot.md`), latest versions, JPA (`0010`). Auth is GitHub OAuth (`0006`, plain OAuth app `0011`) with JWT, ES256, Docker secrets (`0009`, `0015`). Migrations: Flyway (`0007`)
- Python: upstream collector only
- Vite.js with React and TypeScript: frontend (`docs/decisions/0002-react-frontend-framework.md`, `0008`)
- PostgreSQL: main database. Redis: token state only, JWT blacklist, refresh-token cache and rate-limit counters (`0012`)
- RabbitMQ: scan jobs. Kafka: runtime events (stage 6, later)
- Nginx reverse proxy, Docker

## Decisions already made

- Usage detection is hybrid, built in stages: declare, then repo scan, then runtime observation
- Runtime path uses a separate aggregator service, not the scan service
- CVE catalog is a non-goal
- Postgres is the system of record. No further datastore until a measured problem needs one. One exception, chosen by the owner: Redis for token state only, the JWT blacklist, refresh-token cache and rate-limit counters (`docs/decisions/0012-redis-token-blacklist.md`)
- Schema conventions: `docs/decisions/0013-schema-conventions.md`. Frontend stack: `docs/decisions/0014-frontend-stack.md`
- Monorepo, one folder per service: `api/`, `scan/`, `aggregator/`, `collector/`, `web/`. Compose and `nginx/` at root (`docs/decisions/0004-monorepo.md`)

## Working rules

- **Never add `Co-Authored-By` or any attribution line to git commits or PR descriptions.** This overrides any default.
- Commit doc changes without asking. The owner will say if a revert is needed.
- The owner makes planning, architecture, stack, and hosting decisions. Offer options with tradeoffs when asked. Do not decide for them, and do not suggest new project ideas or scope unless asked.
- Record decisions in `docs/decisions/` when the owner makes them.
- Anything undecided stays marked as open in the docs. Do not fill gaps with guesses.
- Competitor and vendor facts in the docs come from memory and must be verified before relying on them.

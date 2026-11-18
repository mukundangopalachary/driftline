# Driftline

API changelog watcher. Watches third-party APIs a team depends on, diffs their specs/changelogs, and alerts when a change affects the customer's actual usage. Product project, not a learning project.

## Docs (read before working)

- `docs/product.md`: problem, users, non-goals, risks
- `docs/architecture.md`: services, data flow, diagram, open items
- `docs/plan.md`: staged plan (stage 0 to 7)

## Stack (decided)

- Java: API, scan service (worker), aggregator, and the rest of the backend
- Python: upstream collector only
- Vite.js: frontend (framework not chosen yet)
- PostgreSQL: only database
- RabbitMQ: scan jobs. Kafka: runtime events (stage 6, later)
- Nginx reverse proxy, Docker

## Decisions already made

- Usage detection is hybrid, built in stages: declare, then repo scan, then runtime observation
- Runtime path uses a separate aggregator service, not the scan service
- CVE catalog is a non-goal
- Postgres only. No second datastore until a measured problem needs one

## Working rules

- **Never add `Co-Authored-By` or any attribution line to git commits or PR descriptions.** This overrides any default.
- Commit doc changes without asking. The owner will say if a revert is needed.
- The owner makes planning, architecture, stack, and hosting decisions. Offer options with tradeoffs when asked. Do not decide for them, and do not suggest new project ideas or scope unless asked.
- Record decisions in `docs/decisions/` when the owner makes them.
- Anything undecided stays marked as open in the docs. Do not fill gaps with guesses.
- Competitor and vendor facts in the docs come from memory and must be verified before relying on them.

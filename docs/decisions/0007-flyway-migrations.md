# 0007: Flyway for database migrations

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

Use Flyway for Postgres schema migrations.

## Not decided here

- Schema conventions (naming, ids, timestamps, soft delete)
- Which service owns migrations (the Python collector also writes to Postgres, see open items in `architecture.md`)
- Where migration files live in the monorepo

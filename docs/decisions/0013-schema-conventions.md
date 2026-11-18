# 0013: Postgres schema conventions

- Date: 2026-11-18
- Decided by: Claude, delegated by the owner ("schema conventions u decide"). Owner can change any of these.
- Status: accepted

## Conventions

1. **Naming:** `snake_case`. Table names plural (`users`, `projects`). Columns singular. Foreign key column is `<singular>_id` (`project_id`).
2. **Primary keys:** `uuid`, default `gen_random_uuid()`. Reason: safe to expose in URLs, no enumeration. Tradeoff: bigger indexes than bigint, random insert order. Acceptable at this scale.
3. **Timestamps:** `timestamptz`, stored UTC. Every table has `created_at not null default now()`. Mutable tables also have `updated_at`, maintained by the application (JPA), not triggers.
4. **Enums:** `text` with a `check` constraint, not native Postgres enums. Reason: easier to change in migrations.
5. **Constraints:** name them explicitly (`pk_`, `fk_`, `uq_`, `ck_` prefixes). Index every foreign key. Declare `not null` by default.
6. **Deletes:** hard delete by default. No soft delete unless a table needs it (record why in its migration). Use `on delete` rules deliberately, not left implicit.
7. **Money, counts, versions:** use exact types. No floats for anything countable.
8. **Flyway:** migrations in `api/src/main/resources/db/migration`, named `V<n>__<description>.sql`. Never edit an applied migration. JPA runs with `ddl-auto=validate`, never `update`.
9. **Ownership:** the `api` service owns the schema and all migrations. The Python collector and other services write only to tables the schema defines and do not run migrations. How the collector writes (direct Postgres, queue, or API) is still open in `architecture.md`.
10. **Source links:** any table storing an upstream change must have columns for source URL and quoted source text (from `plan.md` cross-cutting rules).

## Not decided here

- Specific tables (written in the Stage 1 and 2 migrations)
- Multi-tenancy model beyond project ownership
- Retention and deletion policy (Stage 5)

## Applied (2026-11-18)

`api/src/main/resources/db/migration/V1__users_and_refresh_tokens.sql` follows these conventions: UUID keys with `gen_random_uuid()`, `timestamptz`, named `pk_`, `uq_`, `fk_` constraints, an index on the foreign key (`ix_` prefix for plain indexes, added here as a naming choice). `refresh_tokens` references `users` with `on delete cascade`, a deliberate rule so deleting a user removes their tokens.

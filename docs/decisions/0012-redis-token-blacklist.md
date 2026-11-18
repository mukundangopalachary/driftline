# 0012: Redis for the JWT blacklist

- Date: 2026-11-18
- Decided by: owner
- Status: accepted
- Amends: the earlier rule "Postgres only. No second datastore until a measured problem needs one" (in `CLAUDE.md`)

## Decision

Add Redis as a second datastore, used for the JWT revocation blacklist (see `0009-jwt-sessions.md`). Postgres stays the system of record for everything else.

## Why this is an exception

The earlier rule asked for a measured problem first. The owner chose Redis up front for fast blacklist lookups with automatic expiry. Postgres could also do this (a table plus an expiry cleanup), so this is a preference, not a measured need.

## Scope

Redis is for the token blacklist only. Any other use needs its own decision. It is not a general cache yet.

## Not decided here

- Redis version, persistence settings (what happens to the blacklist on Redis restart)
- Behavior when Redis is down (reject all tokens, or accept unchecked)
- Stage when Redis enters Docker Compose (expected Stage 1, when `api/` is scaffolded)
- Whether Redis in production is self-hosted or managed (hosting is open)

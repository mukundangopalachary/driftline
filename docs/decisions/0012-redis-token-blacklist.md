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

Redis is for token state and rate limiting only: the blacklist, the refresh-token cache, and the per-user request counters for the 300 requests per minute limit (see `0009-jwt-sessions.md`). Any other use needs its own decision. It is not a general cache.

## Decided later the same day (owner)

- The blacklist **survives Redis restarts**. Needs Redis persistence turned on (AOF or snapshots); the exact setting is an implementation detail to pick when Redis is added to Compose.
- If Redis is down, requests are **rejected** (fail closed).
- Redis also holds cached refresh tokens (lookup order: Redis, then Postgres), see `0009-jwt-sessions.md`. This widens the scope above from "blacklist only" to "blacklist and refresh-token cache".

## Not decided here

- Redis version policy beyond the first pick: `redis:8` (latest major on Docker Hub, 2026-11-18)
- Stage when Redis enters Docker Compose (expected Stage 1, when `api/` is scaffolded)
- Whether Redis in production is self-hosted or managed (hosting is open)

## Amended later the same day (owner): rate-limit counters

Redis also stores the per-user request counters for the rate limit in `0009`. Counters are short-lived (one-minute windows) and need no persistence beyond what is already set for the blacklist. If Redis is down, requests are rejected, same as for token checks.

## Scaffold notes (2026-11-18)

Redis runs in `docker-compose.yml` as `redis:8` with `--appendonly yes` (append-only file persistence) and a named volume, so the blacklist survives restarts. Verified: `appendonly yes`, `PONG`, healthy. How well AOF fits (fsync policy, data loss window) is not tuned. Default `everysec` applies.

# 0010: Latest Java and Spring, JPA for data access

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

- Use the latest stable Java and Spring Boot versions available when each service is scaffolded. Pin the exact versions in the build files at that time and note them here.
- Use JPA (Spring Data JPA) for Postgres access in the Java services.

## Versions

Not pinned yet. Fill in when `api/` is scaffolded. Check the official release pages first; do not use remembered version numbers.

## Not decided here

- Whether to prefer an LTS Java release over the newest
- JPA provider settings, and where plain SQL is acceptable

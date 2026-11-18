# 0011: Plain GitHub OAuth app for login

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

Use a plain GitHub OAuth app for sign-in. Login does not share a GitHub App with Stage 4 repo scanning.

## Consequences

Stage 4 repo access (read-only) will need its own GitHub App, to be decided then.

## Not decided here

- Scopes requested at login
- Account linking and user deletion rules

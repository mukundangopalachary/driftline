# 0006: GitHub OAuth for authentication

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

Users sign in with GitHub OAuth. No own email and password accounts at launch.

## Options considered

- Own accounts: most build effort and security burden, needed for non-GitHub users.
- OAuth providers: low effort, provider handles credentials.
- Hosted auth service: lowest effort, adds vendor, cost, lock-in.

## Consequences

- Users without a GitHub account cannot sign in. Accepted for now.
- Fits Stage 4 (GitHub app for repo scan).
- Own accounts or more providers can be added later.

## Not decided here

- Sessions vs JWTs
- OAuth app vs GitHub App for login (Stage 4 repo access may use a separate GitHub App, open)
- Which GitHub scopes are requested at login
- Account linking and user deletion rules

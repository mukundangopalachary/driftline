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

## Setup (done by the owner, once)

1. GitHub, Settings, Developer settings, OAuth Apps, New OAuth App.
2. Homepage URL: `http://localhost:5173`
3. Authorization callback URL: `http://localhost:5173/api/auth/oauth2/callback/github`
   (It must match exactly. The API builds it from the request host, and the Vite dev server passes the browser's host through.)
4. Create a client secret. Put `GITHUB_CLIENT_ID` and `GITHUB_CLIENT_SECRET` in the git-ignored `.env`.
5. Production will need a second OAuth app with the real domain. Hosting is open.

## Implemented (2026-11-18)

- Scope requested: `read:user` only (login, name, avatar). No email, no repo access.
- Login starts at `/api/auth/oauth2/authorization/github`, GitHub returns to `/api/auth/oauth2/callback/github`. Both live under `/api` so the dev proxy and Nginx need one rule.
- On success the API creates or updates the user by GitHub id (`users.github_id`), issues both cookies, and redirects to the frontend.
- Not tested against real GitHub yet. The tests feed the success handler what GitHub would return.

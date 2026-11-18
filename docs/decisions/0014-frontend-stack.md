# 0014: Frontend stack choices

- Date: 2026-11-18
- Decided by: Claude, delegated by the owner ("frontend im leaving it upto you"). Owner can change any of these.
- Status: accepted

Builds on `0002-react-frontend-framework.md` and `0008-typescript-frontend.md` (Vite, React, TypeScript).

## Choices

| Area | Choice | Reason |
|---|---|---|
| Package manager | pnpm | Fast, strict dependency resolution, good monorepo support |
| Routing | React Router | Most common, enough for login, projects, feed, alerts |
| Server state / data fetching | TanStack Query | Caching, retries, loading and error state for API calls |
| UI | Tailwind CSS with shadcn/ui components | Components are copied into the repo, not a locked dependency. Fits dashboard screens |
| Tables | TanStack Table, added when the change feed needs it (Stage 3) | Not needed in Stage 1 |
| Forms | React Hook Form with Zod validation | Typed forms for declare-dependencies screen |
| Lint and format | ESLint and Prettier | Standard |
| Tests | Vitest and Testing Library | Native to Vite |
| Global client state | None at first. Add only if needed | Avoid early state libraries |

## Auth interaction

Token storage in the browser is open (see `0009-jwt-sessions.md`). The frontend will not pick it alone, since it affects the API.

## Not decided here

- Design look and feel, color, branding
- Charts library (Stage 3 dashboard, if any)
- Internationalization

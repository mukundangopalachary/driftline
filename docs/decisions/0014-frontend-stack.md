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
| Lint and format | oxlint and Prettier (changed from ESLint, see below) | oxlint is what the current Vite template ships and it works with TypeScript 7 |
| Tests | Vitest and Testing Library | Native to Vite |
| Global client state | None at first. Add only if needed | Avoid early state libraries |

## Auth interaction

Both tokens live in httpOnly cookies (see `0009-jwt-sessions.md`), so frontend JavaScript never reads them. Every state-changing request must send the CSRF token (Spring's cookie repository: read the `XSRF-TOKEN` cookie, send it in the `X-XSRF-TOKEN` header).

## Not decided here

- Design look and feel, color, branding
- Charts library (Stage 3 dashboard, if any)
- Internationalization

## Scaffold notes (2026-11-18)

- Linter changed from ESLint to **oxlint** (Claude's choice, delegated). The Vite template ships oxlint. I did not want to add ESLint plus typescript-eslint on top of TypeScript 7 without checking compatibility. Prettier is added. Switch back to ESLint if you prefer.
- Versions came from the npm registry on the scaffold date: Vite 8.3, React 19.3, TypeScript 7.0, pnpm 12.21, React Router 8.5, TanStack Query 5, Tailwind 4.3, Vitest 5.1, Zod 4.6, React Hook Form 7.92. Exact versions are pinned by `web/pnpm-lock.yaml`.
- Tailwind is wired in. **shadcn/ui is not initialised yet**; its CLI is run when the first real screens need components.
- Dev server proxies `/api` to the API on port 8082 so the browser sees one origin, as Nginx will provide later. Cookie auth and CSRF depend on this.

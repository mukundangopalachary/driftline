# Driftline: Staged Plan

> Status: DRAFT. Written by Claude from decisions so far. Stage order, scope, and exit criteria are suggestions for the owner to edit. No dates, because pace depends on the owner.

## Principles

1. **Riskiest first.** Biggest risks are alert quality and source coverage, not UI.
2. **End-to-end early.** Get a thin working path (vendor change to alert) before deepening any one part.
3. **Each stage ends with something usable and a clear exit check.**
4. **Hybrid rollout matches the decision:** declare (stage 1 of usage), then repo scan, then runtime observation.
5. **Add infrastructure when a stage needs it.** Do not stand up Kafka before the runtime stage.

## Stage overview

| Stage | Name | Outcome |
|---|---|---|
| 0 | Validate and scaffold | Evidence people want it, repo and local environment ready |
| 1 | Core skeleton | Auth, projects, declare APIs, UI shell on Postgres |
| 2 | Upstream collector (Tier 1) | Real vendor changes captured and classified for a few vendors |
| 3 | Impact matching and alerts | End-to-end alerts at vendor/version level. First usable product |
| 4 | Repo scan | Endpoint-level impact via RabbitMQ and scan service |
| 5 | Hardening and beta | Safe, observable, deployable for outside users |
| 6 | Runtime observation | Kafka and aggregator for traffic-metadata-based confidence |
| 7 | Expansion | More sources, vendors, and billing |

Stages 0 and 1 can overlap. Stage 6 is deliberately late and optional until users ask for it.

---

## Stage 0: Validate and scaffold

> Status: CLOSED 2026-11-18. Validation skipped by owner (`decisions/0003-skip-stage-0-validation.md`). Scaffold done and verified. Hosting direction still open, can wait.

**Goal:** Confirm the pain is real and set up a place to build.

**Work**
- ~~Run the validation plan in `product.md`: list 10 contacts, talk to at least 5, ask for a recent API-change incident.~~ Skipped, see `decisions/0003-skip-stage-0-validation.md`.
- Check for direct competitors in this niche. Done, see `competitors.md`.
- ~~Manual pilot: hand-watch about 5 APIs for one friendly team for 2 weeks and see if alerts get acted on.~~ Skipped, see `decisions/0003-skip-stage-0-validation.md`.
- Scaffold: monorepo (decided, `decisions/0004-monorepo.md`), Docker Compose with Postgres and Nginx. Done.

**Decisions needed**
- ~~Repo layout~~ decided: monorepo, see `decisions/0004-monorepo.md`
- ~~Java framework~~ decided: Spring Boot, see `decisions/0005-spring-boot.md` (build tool: Gradle, `decisions/0001-gradle-java-build-tool.md`)
- ~~Frontend framework on Vite~~ decided: React, see `decisions/0002-react-frontend-framework.md`
- Hosting direction (can wait, but affects Docker choices)

**Exit criteria**
- ~~At least 3 people describe a real incident or near-miss~~ Skipped
- ~~At least 1 alert from the manual pilot was acted on~~ Skipped
- `docker compose up` brings up Postgres and Nginx. Verified 2026-11-18: Postgres healthy, Nginx `/healthz` returns 200 on host port 8081 (8080 was taken by another local container)

**Risks:** Skipping validation because building feels more productive. The owner chose to skip it (`decisions/0003-skip-stage-0-validation.md`).

---

## Stage 1: Core skeleton

> Progress 2026-11-18: `api/` and `web/` scaffolded and verified (builds, tests pass, API boots against compose, dev proxy works). Redis in compose. Not built yet: GitHub OAuth login, JWT flow, Postgres schema v1 (users, projects, vendors, declared usage), the declare screen, Nginx routing to API and web, CI.

**Goal:** A logged-in user can create a project and declare the APIs and versions they use.

**Work**
- API service (Java): auth, projects, declared dependencies (vendor, API version)
- Postgres schema v1: users, projects, vendors, declared usage
- Frontend (Vite): login, project list, declare-dependencies screen
- Nginx routes frontend and API
- Basic CI: build, test, lint

**Decisions needed**
- ~~Auth approach~~ decided: GitHub OAuth, see `decisions/0006-github-oauth-auth.md`. JWT access tokens (1 day) with refresh tokens (30 days, Redis and Postgres), revocation and a Redis blacklist (`0009`, `0012`), plain OAuth app (`0011`). Signing: ES256 with Docker secrets (`0015`). Refresh token: opaque base64, replaced at expiry, httpOnly cookie (`0009`). CSRF token, forced logout on excessive requests (`0009`). The 14-day inactivity rule is deferred to near the end of the project. Dev keys via openssl in git-ignored `.env` (`0015`). Refresh token hashed in storage, both tokens in httpOnly cookies, Spring Security CSRF, 300 req/min forced-logout limit, 7 revocation triggers (`0009`). Rate limit applies to authenticated endpoints only. Rate counters live in Redis (`0012`). Still open: key rotation
- ~~Schema conventions~~ decided: `0013`. Migration tool: Flyway, `decisions/0007-flyway-migrations.md`
- Vendor registry shape (shared with Stage 2)

**Exit criteria**
- Create account, project, and declare 3 vendors end to end through the UI
- Everything runs in Docker locally
- Migrations repeatable from empty database

**Risks:** Over-building auth, billing, or admin screens early.

---

## Stage 2: Upstream collector (Tier 1)

**Goal:** Reliably capture real changes from a few vendors.

**Work**
- Python collector service
- Curated source registry: spec URL, GitHub repo, changelog or feed per vendor
- Tier 1 sources: OpenAPI spec diff, GitHub releases and tags, SDK changelogs
- Change model in Postgres: type (breaking, deprecation, additive, docs-only), severity, affected endpoints or fields where known, effective date, source link and quoted source text
- Deterministic classification for spec diffs
- LLM extraction for unstructured changelog text, gated by a confidence score, with low-confidence items held for manual review
- Scheduler with conditional requests and polite frequency

**Decisions needed**
- Registry format and where it lives (file in repo vs table)
- How the collector writes to the Java side (direct Postgres, queue, or API). Listed as open in `architecture.md`
- First 3 vendors (prefer ones with public OpenAPI specs)
- LLM provider and where its output is stored

**Exit criteria**
- 3 vendors monitored for 2+ weeks without manual fixes
- Every stored change links to original source text
- Replaying a known historical breaking change produces the expected classification
- Source breakage (404, format change) is detected, not silent

**Risks:** Noisy diffs (cosmetic changes), vendors moving URLs, LLM misclassification. This stage decides whether alert quality is achievable.

---

## Stage 3: Impact matching and alerts

**Goal:** Close the loop with declared usage. First end-to-end product.

**Work**
- Impact matcher: join declared usage with classified changes (vendor and version level)
- Alert model with confidence label (`declared`)
- Notifier: email first, then one more channel (Slack or webhook)
- Dashboard: change feed, alert list, deadline view for deprecations and sunsets
- Alert controls: acknowledge, snooze, mute per vendor

**Decisions needed**
- Matcher and notifier as separate services or inside the API (open in `architecture.md`)
- Alert deduplication and grouping rules
- Digest vs immediate alerts

**Exit criteria**
- A real vendor change produces a real alert to a real inbox
- Duplicate or repeated alerts are suppressed
- Pilot users from Stage 0 try it and give feedback

**Risks:** Vendor-level alerts alone may feel like a changelog feed. Expect this and move on to Stage 4.

---

## Stage 4: Repo scan (RabbitMQ and scan service)

**Goal:** Endpoint-level impact: "you call the removed endpoint, here is the file and line."

**Work**
- RabbitMQ in Docker Compose
- GitHub app integration (read-only repo access)
- API enqueues scan jobs, scan service (Java worker) pulls, scans, writes usage map, acks, then pulls the next
- Static analysis for one target language and a small set of vendors, mapping SDK calls and URLs to endpoints
- Usage map in Postgres with confidence label (`found in code`)
- Rescan on push (webhook) and on demand
- Matcher upgraded to endpoint-level impact

**Decisions needed**
- First scan target language
- How the SDK-to-endpoint mapping is built and maintained (the hard part)
- Clone and cleanup strategy for untrusted repos
- Retry, dead-letter, and idempotency policy for scan jobs

**Exit criteria**
- Scan a real repo and correctly find usage for at least 2 vendors
- Alert includes file and line reference
- Killing a worker mid-scan loses no jobs, and the job is retried
- The same repo scanned twice produces the same usage map

**Risks:** Static analysis misses (dynamic URLs, wrapper layers) cause false negatives. Security review friction over repo access. Scan time and consumer timeouts for large repos.

---

## Stage 5: Hardening and beta

**Goal:** Safe and observable enough for outside users.

**Work**
- Scan worker isolation: read-only, never execute repo code, size and time limits, ephemeral disk
- Secrets handling for tokens and credentials
- Observability: structured logs, metrics (queue depth, scan duration, collector success rate), alerting on the system itself
- Failure drills: kill workers, break a vendor source, fill queue
- Backups and restore test for Postgres
- Rate limits, input validation, audit trail for access to customer repos
- Deployment: choose hosting, CI/CD, TLS
- Documentation and onboarding flow

**Decisions needed**
- Hosting and environments
- Data retention and deletion policy for scanned data
- Privacy stance for customer code (what is stored: metadata only, or snippets)

**Exit criteria**
- Restore from backup tested
- Failure drills pass with no data loss
- 3 external users on the beta for 2+ weeks
- Security checklist reviewed

---

## Stage 6: Runtime observation (Kafka and aggregator)

**Goal:** Highest-confidence usage from real traffic metadata.

**Work**
- Kafka in Docker Compose
- Collection method for runtime metadata (SDK, log ingestion, or proxy; open in `architecture.md`). Metadata only, no bodies.
- Aggregator service (Java) consumes events, rolls up per endpoint and version: call counts, last seen, versions
- Capture `Deprecation` and `Sunset` headers seen in traffic
- Alerts prioritized and labeled by `seen in traffic`

**Decisions needed**
- Collection method and its security and PII posture
- Retention and rollup windows
- Whether volume justifies a time-series store (default stays Postgres)

**Exit criteria**
- One real application sends metadata and produces a rolled-up usage map
- Alerts show traffic volume and last-seen
- Aggregator survives restarts without losing or double-counting beyond documented limits

**Risks:** Heaviest trust ask. Optional until users request it.

---

## Stage 7: Expansion

**Goal:** Grow coverage and monetize.

**Candidates (pick by demand)**
- Tier 2 and 3 sources: changelog feeds, docs-page diffing with headless browser, vendor emails, status pages
- Assisted vendor-source discovery
- More vendors and more scan languages
- Issue-tracker integration (create ticket per deprecation with deadline)
- Billing and plans
- Optional public API-change feed as a growth channel

---

## Cross-cutting concerns (every stage)

- **Tests:** deterministic parts (spec diff, matching) get thorough tests. LLM extraction gets an evaluation set of real changelogs with expected output.
- **Source links:** every claim traceable to original upstream text.
- **Low noise:** track precision of alerts, and treat false positives as bugs.
- **Docs:** record each decision in `docs/decisions/` as it is made.
- **Commit docs with changes.**

## Open items carried from `architecture.md`

- Collector-to-Java handoff method
- Matcher and notifier placement
- Scan worker safety limits and idempotency
- RabbitMQ consumer timeout and heartbeat for long scans
- Runtime collection method
- First scan language

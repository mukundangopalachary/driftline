# Driftline: Product

> Status: DRAFT. Product definition only. No architecture, stack, or hosting decisions are made here; those are the owner's.

## One-line pitch

Driftline watches the third-party APIs your product depends on and tells you, before production breaks, when a change affects the calls you actually make.

## The problem

Every product depends on APIs it does not control: payments, messaging, auth, AI providers, shipping, CRMs. These APIs change constantly:

- Fields get deprecated, renamed, or removed
- Default behavior changes
- Versions get sunset on a date buried in a changelog
- Rate limits, pricing, and error formats change
- Docs change without a clear announcement

Teams find out in one of three bad ways:

1. Production breaks and customers notice first
2. Someone happens to read a vendor email or changelog
3. A deprecation notice arrives with a deadline nobody tracked

Existing tools cover adjacent problems (uptime monitors, CVE scanners, generic page-change trackers) but do not answer the real question: **"Does this change affect us?"**

## Core idea

Two halves joined together:

1. **What changed upstream**: detect changes in an API's spec, docs, changelog, and announcements.
2. **What we actually use**: know which endpoints, fields, and versions the customer's code calls.

Value lives in the join. A raw changelog feed is noise. "Stripe is removing field X on March 1, and you read X in 3 places" is an alert people act on.

## Target user

Primary: small to mid-size engineering teams (2 to 50 engineers) with many third-party integrations and no dedicated platform or vendor-management function.

Likely first segments to test:

- Startups heavily dependent on payment, messaging, or AI APIs
- Agencies maintaining integrations for many clients
- Solo developers and indie hackers with a few critical dependencies

Not the target for now:

- Large enterprises with formal vendor-risk processes
- Teams monitoring their own internal APIs (different problem)
- API providers wanting to publish changelogs (opposite side of the market)

## What the user gets

- A list of APIs they depend on, with versions in use
- A stream of upstream changes, classified by type (breaking, deprecation, additive, docs-only) and severity
- Impact analysis: which of their usage is affected, and where
- Deadline tracking for sunsets and deprecations
- Alerts through the channels they already use (email, Slack, issue tracker)
- A plain-language summary of each change, always linked to the original source

## Key product questions

These shape the product more than any technical choice.

1. **How does Driftline learn what the customer uses?** Options: scan their repository, observe live traffic, have them declare it, or a mix. Each trades off setup friction, accuracy, and trust.
2. **Where does change data come from?** OpenAPI specs, changelog pages, release notes, email announcements, SDK releases. Coverage differs per vendor.
3. **Who curates it?** Fully automatic, or curated for a fixed set of popular APIs first?
4. **What counts as breaking?** Needs a defensible definition, and honest handling of uncertainty.
5. **How are false positives handled?** An alerting product that cries wolf gets muted and then cancelled.

## Differentiation hypotheses

To test, not yet validated:

- **Impact, not just change.** Alerts tied to the customer's real usage.
- **Source-linked summaries.** Every claim cites the exact upstream text, so trust is checkable.
- **Deadline awareness.** Sunset dates become tracked, owned tasks.
- **Low-noise by design.** Fewer, higher-confidence alerts over completeness.

## Non-goals

- Not an uptime or latency monitor
- Not an API gateway or proxy in the request path (at least initially)
- Not a general web-page change tracker
- Not a security vulnerability scanner
- Not a replacement for reading vendor docs on major migrations
- No auto-fixing code in v1

## Competitor and alternative map

Verify all of this before relying on it. It is from memory and the space changes fast.

| Category                       | Examples                                       | Gap                                             |
| ------------------------------ | ---------------------------------------------- | ----------------------------------------------- |
| Generic page-change monitors   | Visualping-style tools                         | No API semantics, no impact analysis            |
| Dependency update bots         | Dependabot, Renovate                           | Cover package versions, not remote API behavior |
| API spec diff tools            | OpenAPI diff tooling, API-governance platforms | Mostly for API owners, not consumers            |
| Vendor changelog feeds / email | Per-vendor RSS, newsletters                    | Manual, scattered, no impact mapping            |
| Uptime and synthetic monitors  | Status and monitoring platforms                | Detect outages, not upcoming breaking changes   |

Direct competitors exist for the change-watching half (changelog and spec monitors with Slack and webhook alerts). No tool was found that maps changes to the customer's own usage. Details, sources, and caveats in `competitors.md`. Still unverified by hands-on trial.

## Risks

1. **Source coverage.** Many vendors have no machine-readable spec or structured changelog. Unstructured sources are hard to parse reliably.
2. **Accuracy.** Misclassifying a breaking change as safe (or the reverse) damages trust quickly.
3. **Usage detection friction.** If learning customer usage needs heavy setup or repo access, adoption slows and security reviews start.
4. **Cold start.** Without usage mapping the product degrades into a changelog feed, which is a commodity.
5. **Vendor long tail.** Customers depend on obscure APIs you do not cover.
6. **Willingness to pay.** Pain is real but episodic. Need to confirm teams pay for prevention.

## Open questions (owner decides)

- First vertical or API set to cover (payments? AI providers? a curated top 20?)
- Usage detection approach
- Change-source strategy (specs first, or docs/changelog parsing first)
- Where AI is used, and where deterministic logic must be used
- Pricing unit (APIs watched, seats, projects)
- Self-serve vs sales-assisted
- Public data layer (a free public "API change feed") as a growth channel?

## Validation plan

Before building much:

1. List 10 teams or developers you could talk to.
2. Ask: "Tell me about the last time a third-party API change broke something or nearly did."
3. Ask how they track deprecations today and what it cost them.
4. Check whether they would connect a repo, and what would block them.
5. Manually build a one-off for one friendly team: watch 5 APIs for them by hand for 2 weeks and see whether they act on the alerts. Learn the alert quality problem before writing software.

## Success signals (early)

- Several teams recall a concrete incident or near-miss without prompting
- A manual pilot produces at least one alert a team acts on
- A user asks "can you add API X?" unprompted
- Someone offers to pay before the product is polished

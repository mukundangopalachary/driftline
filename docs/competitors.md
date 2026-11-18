# Driftline: Competitor Check

> Status: DRAFT. Web search on 2026-11-18. Sources are vendor pages, blog posts, and marketplace listings. **None of it is independently verified.** Pricing, coverage, and accuracy claims are the vendors' own. Replaces the "unknown to me" note in `product.md` with evidence, but each tool below still needs a hands-on trial.

## Verdict

Direct competitors exist. The niche is not empty. Several tools already do "watch third-party API changelogs and specs, classify breaking vs non-breaking, alert via Slack or webhook." The search found **no tool that maps a change to the customer's own code or traffic**. That is the join `product.md` calls the core value. This is the only gap seen so far, and it is unconfirmed (absence of search results is weak evidence).

## Tools found

| Tool | What it claims | Source |
|---|---|---|
| APIDrift | Monitors 26+ API, SDK, library changelogs for breaking changes. Slack and webhook alerts, deprecation calendar | https://apidrift.dev/ |
| DeprecationWatch | Deprecation alerts across a catalog of APIs. Coverage and speed claims unverified | https://www.deprecationwatch.com/ |
| API Breaking Change Radar (Apify actor) | OpenAPI diff plus changelog monitor, severity grades (breaking / deprecation / additive), webhooks | https://apify.com/travelmonitorlab/api-breaking-change-radar |
| API Changelog Monitor / Changelog Triage Agent (Apify actor) | Classifies changelog entries BREAKING / WARNING / INFO across services | https://apify.com/joeslade/changelog-triage-agent |
| OpenAPI Change Monitor (Apify actors) | Scheduled spec polling, breaking-change rows to Slack, email, webhook | https://apify.com/prophed/openapi-change-monitor |
| API Change Monitor MCP server | Changelog entries tagged deprecation or breaking, status incidents, OpenAPI diffs, Sunset header reads | https://github.com/mambalabsdev/mcp-api-change-monitor |
| FlareCanary, API Drift Alert (~$149/mo claimed), APIShift | Continuous third-party schema drift monitoring. Listed in a 2026 comparison post | https://dev.to/flarecanary/api-schema-drift-detection-tools-compared-2026-1ib4 |
| PageCrawl and generic page monitors | Watch changelog and deprecation pages, hour-level latency claimed, no API semantics | https://pagecrawl.io/blog/saas-api-deprecation-sunset-notice-monitoring |
| oasdiff | Open source OpenAPI diff. Documents a snapshot-and-diff workflow for third-party specs, with CI | https://www.oasdiff.com/docs/monitor-external-apis |

Also seen: vendor-native feeds (RSS, Atom, JSON Feed changelogs, email lists, forums) and `Deprecation` / `Sunset` response headers (RFC 8594 for Sunset). These are the free baseline Driftline competes against.

## What this means for the docs (observations, owner decides)

- `product.md` risk 4 (cold start degrades into a commodity changelog feed) is confirmed by the market: the feed layer is already crowded and cheap, including free open source diffing.
- Hypotheses still open: "Impact, not just change" has no visible competitor. "Deadline awareness" and "source-linked summaries" are partly claimed by APIDrift (deprecation calendar) and unknown for the rest.
- Source-fetch failure (404, vanished spec) as its own alert type was suggested in one post. Already in the plan's Stage 2 exit criteria.
- Stage 0 exit check "at least 3 people describe a real incident" matters more now: the question is whether teams already use one of these tools and still get hurt.

## Next verification steps

1. Trial the 3 closest: APIDrift, DeprecationWatch, one Apify actor. Record coverage, alert quality, noise, price.
2. Check whether any offers repo connection or usage declaration. Search found none.
3. Ask interviewees which of these they know or use (add to interview script).
4. Pricing: do not rely on any figure above.

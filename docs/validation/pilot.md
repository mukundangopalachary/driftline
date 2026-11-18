# Manual pilot

Goal (from `../product.md`): hand-watch about 5 APIs for one friendly team for 2 weeks. Learn alert quality before writing software.

## Setup

| Field | Value |
|---|---|
| Team (role, size) | |
| Start date | |
| End date (start + 14 days) | |
| Contact for feedback | |
| Alert channel (email, Slack, etc.) | |

APIs watched (about 5, prefer ones with public OpenAPI specs or changelogs):

| # | Vendor / API | Version team uses | Source checked (spec, changelog, GitHub releases) | URL |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |

Usage info: ask the team which endpoints and fields they use per API (declared usage). Record it here, since impact judgement depends on it.

| API | Endpoints / fields used |
|---|---|
| | |

## Routine

- Check each source daily (or every 2 days). Note date checked.
- Only send an alert if the change plausibly affects declared usage, or is a deprecation or sunset with a date.
- Every alert quotes the exact source text and links it.
- Log every change seen, including ones not sent, so noise rate is measurable.

## Change log (every change seen)

| Date | API | Change | Type (breaking / deprecation / additive / docs-only) | Affects declared usage? | Sent? | Source link + quote |
|---|---|---|---|---|---|---|
| | | | | | | |

## Alert log (what was sent)

| Date sent | API | Summary | Team response (acted / ignored / wrong) | Time to response | Notes |
|---|---|---|---|---|---|
| | | | | | |

## Results (fill at end)

- Changes seen: 
- Alerts sent: 
- Alerts acted on: 
- False positives (sent, not relevant): 
- Misses (team found a change we did not): 
- Effort per day (minutes): 
- Sources that were hard to read or parse: 
- Did the team ask for more APIs? 
- Would they pay? 

Exit check: at least 1 alert acted on. Findings feed Stage 2 design (classification, noise) and the open question on usage detection.

# Interview script

20 to 30 minutes. Past behavior only. Do not describe the product until the end.

## Opening
"I'm researching how teams handle changes in the third-party APIs they depend on. No pitch, I want to hear what actually happens."

## Core questions (in order)

1. Which third-party APIs does your product depend on? Which would hurt most if they broke?
2. Tell me about the last time a third-party API change broke something, or nearly did.
   - When did you find out? How? (customer, monitoring, vendor email, by luck)
   - How long to fix? Who got pulled in? Any cost (money, customers, time)?
3. How do you track deprecations and sunset dates today? Who owns it?
4. Do you subscribe to vendor changelogs, emails, or RSS? Do you read them? What happens to them?
5. Have you used any tool to watch API changes? Which? What was missing or annoying? (Prompts: APIDrift, DeprecationWatch, page-change monitors, oasdiff, Dependabot-style bots. See `../competitors.md`.)
6. If a tool told you "vendor X removes field Y on date Z," what would you need to also know to act on it? (Probe: do you know where you use Y? could you find out fast?)
7. Would you connect a repo to a tool like that? What would block it? (security review, policy, trust, setup effort)
8. Would you instead declare your APIs and versions by hand? Tolerable or too much work?
9. What would make you mute or cancel an alerting tool?
10. Does anyone pay for something like this today, or would you? Who decides?

## Close
- Can I send you alerts for 5 APIs you use for 2 weeks and ask if they were useful? (pilot, see `pilot.md`)
- Who else should I talk to?

## Listen for
- Unprompted incident stories (success signal in `../product.md`)
- Cost of the incident in concrete terms
- Repo-connect blockers (risk 3)
- Noise tolerance (risk 2)
- Existing tools and why they failed
- Who owns the problem (buyer persona)

## Call log template

```
Contact #:
Date:
APIs used:
Incident (verbatim quote):
How found out:
Cost:
Current tracking method:
Tools tried:
Repo connect: yes / no / maybe, blocker:
Would pay / who decides:
Pilot agreed: y/n
Notes:
```

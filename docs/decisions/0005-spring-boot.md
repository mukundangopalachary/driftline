# 0005: Spring Boot as Java framework

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

Use Spring Boot for the Java services (API, scan worker, aggregator).

## Options considered

- Spring Boot: largest ecosystem, first-class AMQP and Postgres support. Heavier runtime, framework magic.
- Quarkus, Micronaut: lighter startup and memory, smaller communities.
- Javalin or plain libraries: lightest, but more to assemble by hand.

## Not decided here

- Spring version and Java version
- Data access approach (JPA, JDBC, jOOQ, etc.)
- Migration tool (open, Stage 1)
- Whether all Java services share one Gradle build (see `0004-monorepo.md`)

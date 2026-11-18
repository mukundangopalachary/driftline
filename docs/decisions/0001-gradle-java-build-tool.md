# 0001: Gradle as Java build tool

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

Use Gradle for all Java services (API, scan worker, aggregator).

## Options considered

- Maven: declarative, conventional, widest docs. Slower, verbose.
- Gradle: concise, faster incremental builds, flexible. Steeper curve, build scripts are code.

## Not decided here

- Java framework (open)
- Groovy vs Kotlin build scripts (open)
- Multi-module layout (tied to repo layout, open)

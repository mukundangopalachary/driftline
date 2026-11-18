# 0010: Latest Java and Spring, JPA for data access

- Date: 2026-11-18
- Decided by: owner
- Status: accepted

## Decision

- Use the latest stable Java and Spring Boot versions available when each service is scaffolded. Pin the exact versions in the build files at that time and note them here.
- Use JPA (Spring Data JPA) for Postgres access in the Java services.

## Versions

Pinned 2026-11-18 when `api/` was scaffolded. Looked up from the official sources (Spring Initializr metadata, npm registry, Gradle plugin portal, Docker Hub), not from memory.

| Item | Version | Source |
|---|---|---|
| Spring Boot | 4.1.2 (latest stable offered) | start.spring.io |
| Java | 27 (newest offered; 25 is the LTS) | start.spring.io |
| Gradle | 9.9.1 (wrapper) | generated wrapper |
| Foojay toolchain resolver plugin | 1.0.0 | Gradle plugin portal |

The Java version lives in one place, `api/build.gradle`, `java.toolchain.languageVersion`. Switching to 25 (LTS) is a one-line change.
`api/settings.gradle` applies the Foojay plugin so Gradle downloads the JDK when it is not installed. Developers do not need JDK 27 installed to build.

## Not decided here

- Whether to move to the LTS (Java 25) before production. Java 27 was used because the owner said "all latest".
- Groovy build scripts were used (the Initializr default). Kotlin DSL was open in `0001`; this settles it unless the owner objects (Claude's choice).
- JPA provider settings, and where plain SQL is acceptable

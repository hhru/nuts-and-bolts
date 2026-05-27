# AGENTS.md

## Project Overview

Nuts-and-Bolts (NaB) is a set of small Java libraries used by hh.ru services to build Spring Boot micro-services. It bundles an opinionated Jetty/Jersey web stack, multi-DataSource JDBC and Hibernate support with master/replica routing, Kafka consumer/producer wrappers, OpenTelemetry, Sentry, Consul and structured logging. Consumers depend on `ru.hh.nab:*` artifacts; this repo publishes them, it is not itself a deployable service.

## Tech Stack

- Java 17, Maven (multi-module). Parent: `ru.hh.public-pom:public-pom`
- Jersey 3 (JAX-RS) + Jetty + Spring Framework (web/jdbc/orm/tx). Designed for Spring Boot consumers (integrates via `spring-boot-starter-jersey`)
- Hibernate ORM, PostgreSQL (driver), `ru.hh.jclient-common` (async HTTP RPC), Spring Kafka
- OpenTelemetry (otlp exporter), Sentry, Logback + logstash-logback-encoder, `ru.hh.consul:consul-client`
- Tests: JUnit 5 + Mockito + Spring Boot Test, Testcontainers (PostgreSQL), `ru.hh.kafka.test:hh-kafka-test-utils`

## Build & Test Commands

- Coverage + Sonar (CI, Travis): `mvn -P codecoverage,!sonar clean test sonar:sonar`
- Release a new version: `mvn release:prepare release:perform` (artifacts published to hh Nexus via `maven-release-plugin`)
- Lint: `mvn validate` (hh-checkstyle inherited from `ru.hh.public-pom:public-pom`)

## Architecture

- This is a library set, not a runnable service. Consumers wire NaB modules into their own `@SpringBootApplication`.
- Public API: top-level packages under each module's `ru.hh.nab.*` namespace; classes outside those are implementation detail and may change without a deprecation cycle.
- Key patterns: JAX-RS 3-layer in resources that ship in `nab-web` (`*Resource` with `@Path` via Jersey); `@ExecuteOnDataSource` aspect in `nab-data-source` for master/replica routing of JDBC calls.

## Modules

- `nab-common/` — shared utilities (timing logger, deadline-context, common types)
- `nab-web/` — Jetty + Jersey integration, Spring web wiring, status resource, exception mappers
- `nab-data-source/` — JDBC DataSource with master/replica, `@ExecuteOnDataSource` aspect, JDBC telemetry hooks
- `nab-jpa/` — JPA + Spring transaction integration
- `nab-hibernate/` — Hibernate persistence provider on top of `nab-data-source`
- `nab-jclient/` — `jclient-common` (async HTTP RPC) integration
- `nab-kafka/` — Kafka consumer/producer wrappers built on Spring Kafka
- `nab-logging/` — Logback config + logstash-logback-encoder + syslog appender
- `nab-metrics/` — StatsD metrics (java-dogstatsd-client)
- `nab-sentry/` — Sentry integration (sentry-logback)
- `nab-consul/` — Consul service discovery (`ru.hh.consul:consul-client`)
- `nab-telemetry/` — OpenTelemetry base (SDK + OTLP exporter)
- `nab-telemetry-servlet/` — OTel instrumentation for servlets
- `nab-telemetry-jclient/` — OTel instrumentation for jclient
- `nab-telemetry-jdbc/` — OTel instrumentation for JDBC
- `nab-telemetry-kafka/` — OTel instrumentation for Kafka
- `nab-testbase/` — test infrastructure: Testcontainers-backed PostgreSQL, JUnit 5 helpers, Kafka test utils

## Code Conventions

- Public API changes (anything an hh.ru service may already import) require a deprecation cycle: keep the old symbol with `@Deprecated` for at least one minor release before removal. Bump `CHANGELOG.md` / `changelog.yaml` accordingly.
- Naming and formatting are enforced by hh-checkstyle from the parent pom — do not duplicate those rules here.

## Testing

- JUnit 5 + Mockito; Spring Boot Test where the module wires Spring beans (`nab-web`, `nab-hibernate`, `nab-kafka`, `nab-jclient`).
- Integration tests use Testcontainers (PostgreSQL via `nab-testbase`) and `hh-kafka-test-utils` for in-process Kafka. They live alongside unit tests under each module (e.g. `nab-hibernate/src/test/java`); there is no dedicated `*-integration-tests` module.
- PR requirement: new behavior in a module ships with tests under that module's test source root. Bugfixes ship with a regression test.

## Security

- No personally identifiable data (FIO, email, phone) in logs or test fixtures.
- No tokens, passwords, or hh.ru-internal hostnames committed to the repo — secrets belong in consumer service config, not in NaB defaults.
- Do not log payloads passed through the library by consumers; assume they may carry PII.

## Working Agreements

- **Think before coding.** State assumptions; ask if uncertain. Don't pick silently between interpretations.
- **Simplicity first.** Minimum code that solves the problem; no speculative abstractions, flexibility, or error handling for impossible scenarios.
- **Surgical changes.** Touch only what the task requires. Match existing style. Don't refactor unrelated code.
- **Goal-driven execution.** Define success criteria up front (test that passes, behavior verified). Loop until verified.

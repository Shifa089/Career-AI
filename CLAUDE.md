# CLAUDE.md

Guidance for Claude Code when working in this repository. Read this first — it captures the
architecture, conventions, and build workflow so you don't need to re-scan the whole codebase.

## What this is

**CareerAI** — a production-grade, AI-powered Career Coach Platform. Multi-module Maven monorepo
of Spring Boot microservices using Anthropic Claude for resume parsing, mock interviews, and
semantic job matching.

## Tech stack

- **Java 21**, **Spring Boot 3.4.1**, **Spring Cloud 2024.0.0**, **Spring AI 1.0.0 (GA)**
- Spring Security 6, JWT (JJWT 0.12.6), OAuth2 (Google/GitHub)
- PostgreSQL 16 + pgvector, Flyway, Redis 7, Apache Kafka
- AWS S3 (LocalStack in dev), Anthropic Java SDK 1.2.0
- Observability: Actuator, Micrometer, Prometheus, Grafana, Zipkin (Brave)
- Build/deploy: Maven, Jib (distroless java21), Docker Compose, GitHub Actions, Railway
- Testing: JUnit 5, Testcontainers, ArchUnit, JaCoCo (80% gate under `ci` profile)
- Frontend (separate, not built yet): React 19 + Vite + TailwindCSS 4

## Modules (8)

| Module | groupId:artifactId | Base pkg | Port | Role |
|---|---|---|---|---|
| common-lib | com.careerai:common-lib | `com.careerai.common` | — | Shared DTOs, exceptions, JWT util. JAR, not a boot app. |
| discovery-server | …:discovery-server | `com.careerai.discovery` | 8761 | Netflix Eureka registry |
| config-server | …:config-server | `com.careerai.config` | 8888 | Spring Cloud Config server |
| api-gateway | …:api-gateway | `com.careerai.gateway` | 8080 | Edge router, JWT pre-filter, rate limiting (reactive) |
| auth-service | …:auth-service | `com.careerai.auth` | 8081 | Auth, JWT, OAuth2 Google/GitHub |
| resume-service | …:resume-service | `com.careerai.resume` | 8082 | PDF parsing (Tika/PDFBox), Claude skill extraction, S3 |
| interview-service | …:interview-service | `com.careerai.interview` | 8083 | WebSocket mock interviews, Claude streaming |
| job-match-service | …:job-match-service | `com.careerai.jobmatch` | 8084 | RAG pipeline, pgvector semantic matching, WebClient |

Parent POM: `com.careerai:careerai-platform:1.0.0-SNAPSHOT` (packaging `pom`). All versions and
BOM imports live in the root `pom.xml` `<properties>` / `<dependencyManagement>` — **change
versions there, never in child POMs**.

## Project layout (per service)

```
{module}/src/main/java/com/careerai/{pkg}/
    {Name}Application.java          # @SpringBootApplication (+ @EnableDiscoveryClient on apps)
    controller/ service/ repository/ domain/ dto/ mapper/ config/ security/ exception/
{module}/src/main/resources/
    application.yml                 # base config (env-var driven, sane localhost defaults)
    application-dev.yml             # local dev overrides
    application-prod.yml            # prod overrides (all secrets via env vars)
    db/migration/V{n}__{desc}.sql   # Flyway (JPA services only)
{module}/src/test/java/com/careerai/{pkg}/
    ArchitectureTest.java           # ArchUnit layered-architecture enforcement (app services)
```

`com.careerai.common` (common-lib) holds the cross-service primitives:
`dto/ApiResponse<T>`, `dto/ErrorResponse`, `exception/GlobalExceptionHandler`,
`exception/{ResourceNotFound,BadRequest,Unauthorized}Exception`, `security/JwtUtil`.

## Conventions (follow these when adding code)

- **Packages**: `com.careerai.{service}.{controller|service|repository|domain|dto|mapper|config|security|exception}`.
- **REST responses**: always wrap in `ApiResponse<T>` from common-lib. Static factories:
  `ApiResponse.success(data)`, `ApiResponse.success(msg, data)`, `ApiResponse.error(errorResponse)`.
- **Exceptions**: throw common-lib exceptions; they are handled centrally by `GlobalExceptionHandler`.
  Do not add per-service `@RestControllerAdvice` unless extending behavior.
- **Entities**: UUID primary keys (`gen_random_uuid()` / `pgcrypto`), never `Long`.
- **Migrations**: `V{version}__{description}.sql` in `db/migration`. `ddl-auto: validate` — schema
  changes go through Flyway, not Hibernate.
- **Config**: `application.yml` (YAML, never `.properties`), with `dev`/`prod` profiles. New config
  keys should be env-var driven with localhost defaults: `${VAR:default}`.
- **Mappers**: MapStruct (annotation processor already wired in the parent compiler config).
- **Tests**: unit in `src/test`; integration tests suffixed `*IntegrationTest.java` using
  Testcontainers. Each app service keeps a passing `ArchitectureTest`.
- **AI**: default to the latest Claude models. Anthropic config under `spring.ai.anthropic.*`.

## Build & run

There is **no `mvn` / `mvnw` on PATH**. Use IntelliJ's bundled Maven:

```
"/c/Program Files/JetBrains/IntelliJ IDEA 2026.1.3/plugins/maven/lib/maven3/bin/mvn"
```

`java` on PATH is JDK 21 (correct). Common commands:

| Command | Purpose |
|---|---|
| `mvn clean install` | Build all modules. **`dev` profile is active by default → skips tests + JaCoCo.** |
| `mvn clean verify -P ci` | Full build + tests + JaCoCo 80% line-coverage gate |
| `mvn -pl <module> spring-boot:run` | Run one service |
| `mvn -pl <module> -am package -P docker` | Build container image with Jib |
| `docker compose up -d` | Start infra (4 Postgres, Redis, Kafka, Prometheus, Grafana, Zipkin, LocalStack, Kafka-UI) |

JaCoCo `check` is intentionally skipped on `common-lib`, `discovery-server`, `config-server`.

Run order for a full local stack: `docker compose up -d` → discovery-server → config-server →
api-gateway → the four app services. Copy `.env.example` to `.env` first and fill secrets
(`ANTHROPIC_API_KEY`, `JWT_SECRET`, OAuth client IDs/secrets).

## Infra ports (docker-compose)

postgres-auth 5432 · postgres-resume 5433 · postgres-interview 5434 · postgres-jobmatch 5435 ·
redis 6379 · kafka 9092 · kafka-ui 8090 · prometheus 9090 · grafana 3001 · zipkin 9411 ·
localstack 4566.

## Working agreements

- Keep new code consistent with the patterns above; reuse common-lib rather than re-implementing
  response/exception/JWT plumbing.
- When adding a dependency, add the version to the root POM properties / dependencyManagement and
  reference it version-less in the child.
- After structural or dependency changes, sanity-check with a reactor `compile` (see build table).

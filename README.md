# CareerAI Platform

A production-grade, AI-powered Career Coach Platform built with Spring Boot microservices and
Anthropic Claude. CareerAI parses resumes, runs streaming mock interviews, and performs
semantic job matching via a pgvector-backed RAG pipeline.

## Architecture

```
                                  ┌─────────────────┐
                                  │     Clients     │
                                  │ (React 19 SPA)  │
                                  └────────┬────────┘
                                           │ HTTPS
                                  ┌────────▼────────┐
                                  │   api-gateway   │  :8080
                                  │  (JWT, rate     │
                                  │   limiting)     │
                                  └────────┬────────┘
              ┌────────────┬───────────────┼───────────────┬──────────────┐
              │            │               │               │              │
       ┌──────▼─────┐ ┌────▼──────┐ ┌──────▼──────┐ ┌──────▼───────┐      │
       │   auth-    │ │  resume-  │ │ interview-  │ │  job-match-  │      │
       │  service   │ │  service  │ │  service    │ │   service    │      │
       │   :8081    │ │   :8082   │ │   :8083     │ │    :8084     │      │
       └──────┬─────┘ └────┬──────┘ └──────┬──────┘ └──────┬───────┘      │
              │            │               │               │              │
   ┌──────────┴───┐  ┌─────┴─────┐   ┌─────┴─────┐   ┌─────┴──────┐       │
   │ postgres     │  │ postgres  │   │ postgres  │   │ postgres   │       │
   │  auth :5432  │  │resume:5433│   │intvw :5434│   │jobmtch:5435│       │
   └──────────────┘  └─────┬─────┘   └───────────┘   └─────┬──────┘       │
                           │ S3                            │ pgvector     │
                     ┌─────▼─────┐                   ┌─────▼──────┐       │
                     │ LocalStack│                   │  Claude AI │◄──────┘
                     │  (S3)     │                   │ (Spring AI)│
                     └───────────┘                   └────────────┘

  Cross-cutting infra:
    discovery-server :8761 (Eureka)   config-server :8888 (Spring Cloud Config)
    redis :6379   kafka :9092 / kafka-ui :8090   zipkin :9411
    prometheus :9090   grafana :3001
```

## Tech Stack

| Layer            | Technology                                                        |
|------------------|-------------------------------------------------------------------|
| Language/Runtime | Java 21                                                           |
| Framework        | Spring Boot 3.4.1, Spring Cloud 2024.0.0                          |
| Security         | Spring Security 6, JWT (JJWT 0.12.6), OAuth2 (Google/GitHub)      |
| AI               | Anthropic Claude API, Spring AI 1.0.0                            |
| Persistence      | PostgreSQL 16 + pgvector, Flyway, Spring Data JPA                 |
| Cache            | Redis 7                                                           |
| Messaging        | Apache Kafka                                                      |
| Storage          | AWS S3 (LocalStack in dev)                                        |
| Discovery/Config | Netflix Eureka, Spring Cloud Config                              |
| Observability    | Actuator, Micrometer, Prometheus, Grafana, Zipkin (Brave)        |
| Build/Deploy     | Maven, Jib (distroless), Docker, GitHub Actions, Railway         |
| Testing          | JUnit 5, Testcontainers, ArchUnit, JaCoCo (80% gate)             |
| Frontend         | React 19 + Vite + TailwindCSS 4 (built separately)               |

## Modules

| Module             | Port | Description                                              |
|--------------------|------|----------------------------------------------------------|
| common-lib         | —    | Shared DTOs, exceptions, JWT utils, API response wrappers|
| discovery-server   | 8761 | Netflix Eureka service registry                          |
| config-server      | 8888 | Spring Cloud Config server                               |
| api-gateway        | 8080 | Edge router, JWT pre-filter, rate limiting               |
| auth-service       | 8081 | Auth, JWT, OAuth2 Google/GitHub                          |
| resume-service     | 8082 | PDF parsing, Claude skill extraction, S3 upload          |
| interview-service  | 8083 | WebSocket mock interviews, Claude streaming              |
| job-match-service  | 8084 | RAG pipeline, pgvector semantic matching                 |

## Getting Started

### Prerequisites
- JDK 21
- Maven 3.9+
- Docker + Docker Compose

### Steps

```bash
# 1. Clone
git clone <repo-url> careerai && cd careerai

# 2. Configure environment
cp .env.example .env       # then edit secrets (ANTHROPIC_API_KEY, JWT_SECRET, OAuth, ...)

# 3. Start infrastructure (databases, redis, kafka, observability, localstack)
docker compose up -d

# 4. Build everything
mvn clean install

# 5. Run services (in separate terminals, in this order)
mvn -pl discovery-server spring-boot:run
mvn -pl config-server    spring-boot:run
mvn -pl api-gateway      spring-boot:run
mvn -pl auth-service     spring-boot:run
mvn -pl resume-service   spring-boot:run
mvn -pl interview-service spring-boot:run
mvn -pl job-match-service spring-boot:run
```

Each service activates the `dev` Spring profile by default via its `application-dev.yml`.
Set `SPRING_PROFILES_ACTIVE=prod` for production configuration.

## Service Ports

| Service           | Port | Service           | Port |
|-------------------|------|-------------------|------|
| api-gateway       | 8080 | discovery-server  | 8761 |
| auth-service      | 8081 | config-server     | 8888 |
| resume-service    | 8082 | postgres-auth     | 5432 |
| interview-service | 8083 | postgres-resume   | 5433 |
| job-match-service | 8084 | postgres-interview| 5434 |
| redis             | 6379 | postgres-jobmatch | 5435 |
| kafka             | 9092 | kafka-ui          | 8090 |
| prometheus        | 9090 | grafana           | 3001 |
| zipkin            | 9411 | localstack (S3)   | 4566 |

## Build Commands

| Command                                            | Purpose                                  |
|----------------------------------------------------|------------------------------------------|
| `mvn clean install`                                | Build all modules (dev profile, skips tests) |
| `mvn clean verify -P ci`                           | Full build + tests + JaCoCo 80% gate     |
| `mvn -pl <module> spring-boot:run`                 | Run a single service                     |
| `mvn -pl <module> -am package -P docker`           | Build a container image with Jib         |
| `mvn test -pl <module>`                            | Run a module's tests                     |
| `docker compose up -d`                             | Start local infrastructure               |
| `docker compose down -v`                           | Stop infra and remove volumes            |

## Conventions

- Package layout: `com.careerai.{service}.{controller|service|repository|domain|dto|mapper|config|security|exception}`
- All REST responses wrapped in `ApiResponse<T>` (common-lib); errors via `GlobalExceptionHandler`.
- Every entity uses a `UUID` primary key.
- Flyway migrations: `V{version}__{description}.sql`.
- Config in `application.yml` with `dev`/`prod` profiles.
- Integration tests suffixed `*IntegrationTest.java` (Testcontainers); each service has an ArchUnit test.

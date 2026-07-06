# CareerAI developer convenience targets.
#
# There is no `mvn` on PATH in this environment; point MVN at IntelliJ's bundled
# Maven (override on the command line: `make build MVN=mvn`).
MVN ?= /c/Program Files/JetBrains/IntelliJ IDEA 2026.1.3/plugins/maven/lib/maven3/bin/mvn
COMPOSE ?= docker compose
SERVICES := api-gateway auth-service resume-service interview-service job-match-service

.PHONY: help up down build test test-all clean \
        run-api-gateway run-auth-service run-resume-service run-interview-service run-job-match-service \
        logs-api-gateway logs-auth-service logs-resume-service logs-interview-service logs-job-match-service \
        deploy-staging

help: ## List available targets
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-28s\033[0m %s\n", $$1, $$2}'

## ---- Infrastructure ----
up: ## Start the full local infra + observability stack
	$(COMPOSE) up -d

down: ## Stop the local stack
	$(COMPOSE) down

## ---- Build & test ----
build: ## Build all modules (skip tests)
	"$(MVN)" clean install -DskipTests

test: ## Run unit tests
	"$(MVN)" test -P ci

test-all: ## Run unit + integration tests with coverage gate
	"$(MVN)" verify -P ci

clean: ## Clean Maven output and tear down infra + volumes
	"$(MVN)" clean
	$(COMPOSE) down -v

## ---- Run a single service (dev profile) ----
run-api-gateway: ## Run api-gateway
	"$(MVN)" -pl api-gateway spring-boot:run -P dev
run-auth-service: ## Run auth-service
	"$(MVN)" -pl auth-service spring-boot:run -P dev
run-resume-service: ## Run resume-service
	"$(MVN)" -pl resume-service spring-boot:run -P dev
run-interview-service: ## Run interview-service
	"$(MVN)" -pl interview-service spring-boot:run -P dev
run-job-match-service: ## Run job-match-service
	"$(MVN)" -pl job-match-service spring-boot:run -P dev

## ---- Tail container logs ----
logs-api-gateway: ; $(COMPOSE) logs -f api-gateway
logs-auth-service: ; $(COMPOSE) logs -f auth-service
logs-resume-service: ; $(COMPOSE) logs -f resume-service
logs-interview-service: ; $(COMPOSE) logs -f interview-service
logs-job-match-service: ; $(COMPOSE) logs -f job-match-service

## ---- Deploy ----
deploy-staging: ## Trigger the staging deploy workflow (requires gh CLI)
	gh workflow run ci-cd.yml -f environment=staging

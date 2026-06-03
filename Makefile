MVN_IMAGE := maven:3.9-eclipse-temurin-21
M2_VOLUME := incode-m2
RUN_MVN = docker run --rm -v "$(PWD)":/app -v $(M2_VOLUME):/root/.m2 -w /app $(MVN_IMAGE) mvn -B

.DEFAULT_GOAL := help

.PHONY: help build run test it-test verify lint lint-check docker-build up down logs clean

help: ## List available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

$(M2_VOLUME):
	@docker volume create $(M2_VOLUME) >/dev/null

build: $(M2_VOLUME) ## Compile and package the JAR (in a Maven container, skips tests)
	$(RUN_MVN) clean package -DskipTests

lint: $(M2_VOLUME) ## Apply Spotless (Palantir Java Format)
	$(RUN_MVN) spotless:apply

lint-check: $(M2_VOLUME) ## Verify formatting without changing files
	$(RUN_MVN) spotless:check

test: $(M2_VOLUME) ## Run unit tests (Surefire) in a Maven container
	$(RUN_MVN) test

it-test: $(M2_VOLUME) ## Run integration tests (Failsafe + Testcontainers via dind)
	./scripts/run-tests-in-docker.sh failsafe:integration-test failsafe:verify

verify: $(M2_VOLUME) ## Run unit + integration tests + Spotless check (via dind)
	./scripts/run-tests-in-docker.sh verify

docker-build: ## Build the application Docker image
	docker build -t verification-service:latest .

up: ## Start the full stack (app, dynamodb, prometheus, grafana, loki, promtail)
	docker compose up -d --build

run: ## Start only the app and DynamoDB Local
	docker compose up -d --build app dynamodb

down: ## Stop and remove the stack and volumes
	docker compose down -v

logs: ## Tail logs of all stack services
	docker compose logs -f

clean: $(M2_VOLUME) ## Remove build output and stop the stack
	$(RUN_MVN) clean
	docker compose down -v || true

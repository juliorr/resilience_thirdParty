MVN_IMAGE := maven:3.9-eclipse-temurin-21
M2_VOLUME := incode-m2
RUN_MVN = docker run --rm -v "$(PWD)":/app -v $(M2_VOLUME):/root/.m2 -w /app $(MVN_IMAGE) mvn -B

.DEFAULT_GOAL := help

.PHONY: help build run test it-test verify coverage lint lint-check newman traffic docker-build up down down-volumes logs clean hosts-setup hosts-remove

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

coverage: $(M2_VOLUME) ## Generate JaCoCo coverage reports (unit + integration, via dind)
	./scripts/run-tests-in-docker.sh verify
	@echo "Coverage reports:"
	@echo "  verification-service/target/site/jacoco/index.html (unit)"
	@echo "  verification-service/target/site/jacoco-it/index.html (integration)"
	@echo "  third-party-service/target/site/jacoco/index.html (unit)"
	@echo "  third-party-service/target/site/jacoco-it/index.html (integration)"

newman: ## Run the Postman collection headless with Newman (Docker); assumes the stack is already up (make up)
	docker compose run --rm --no-deps newman

traffic: ## Generate load against /backend-service to exercise the FREE/PREMIUM 503 simulation (stack must be up); override count with REQUESTS=N
	docker compose run --rm --no-deps traffic

docker-build: ## Build both application Docker images (verification + third-party)
	docker build -f verification-service/Dockerfile -t verification-service:latest .
	docker build -f third-party-service/Dockerfile -t third-party-service:latest .

up: ## Start the full stack (app, third-party, dynamodb, prometheus, grafana, loki, promtail)
	docker compose up -d --build

run: ## Start only the app, third-party provider and DynamoDB Local
	docker compose up -d --build app third-party dynamodb

down: ## Stop and remove the stack, keeping data volumes (DynamoDB data persists)
	docker compose down

down-volumes: ## Stop the stack and delete data volumes (wipes DynamoDB data)
	docker compose down -v

logs: ## Tail logs of all stack services
	docker compose logs -f

clean: $(M2_VOLUME) ## Remove build output and stop the stack, keeping data volumes
	$(RUN_MVN) clean
	docker compose down || true

hosts-setup: ## Add '127.0.0.1 app' to /etc/hosts so the Prometheus /targets link opens from the host (needs sudo)
	@grep -qE '^127\.0\.0\.1[[:space:]]+app$$' /etc/hosts \
		&& echo "/etc/hosts already maps 'app' -> 127.0.0.1" \
		|| (echo "127.0.0.1 app" | sudo tee -a /etc/hosts >/dev/null \
			&& echo "Added '127.0.0.1 app' to /etc/hosts")

hosts-remove: ## Remove the '127.0.0.1 app' /etc/hosts entry added by hosts-setup (needs sudo)
	@sudo sed -i '' '/^127\.0\.0\.1[[:space:]]\{1,\}app$$/d' /etc/hosts \
		&& echo "Removed '127.0.0.1 app' from /etc/hosts (if present)"

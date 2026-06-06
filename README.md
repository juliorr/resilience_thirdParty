# Verification Service

A backend service that verifies companies against two third-party providers and persists every
verification for audit. **FREE** and **PREMIUM** providers are mocked *inside the same service*
(reading local JSON seed files), so the FREE→PREMIUM fallback and the simulated `503` outages are
**real HTTP behaviour**, not in-memory simulation.

Built with **Java 21 + Spring Boot 3.5 + Maven**, persisted in **DynamoDB**, fully **containerized**
(app, tests, and the whole observability stack run in Docker), observable with
**Prometheus + Grafana + Loki**, and deployable to **AWS ECS Fargate** via **Terraform + GitHub
Actions**.

## Quick start (≈2 minutes)

Requires **Docker** only (no host JDK/Maven needed).

```bash
make up                 # build the image + start app, DynamoDB, Prometheus, Grafana, Loki, Promtail
```

- Swagger UI Backend: <http://localhost:8080/swagger-ui.html>
- Swagger UI Third Party: <http://localhost:8081/swagger-ui.html>
- Grafana: <http://localhost:3000/d/verification/verification-service?orgId=1&from=now-30m&to=now&timezone=browser&refresh=5s> (anonymous admin)
- Prometheus: <http://localhost:9090/targets>
- DynamoDB UI: http://localhost:8002/tables/verification

> **Host port already in use?** Override any host port without touching the file, e.g.
> `APP_HOST_PORT=18080 GRAFANA_HOST_PORT=13000 make up`. Container ports are unchanged.

In **Grafana → Explore (Loki)**, trace a single verification:

```
{app="verification"} | json | verificationId="<the-guid-lowercased>"
```

The provisioned **Verification Service** dashboard shows 503 rate per source, FREE→PREMIUM fallback
rate, latency per source, and result distribution.

Stop everything: `make down`.

## Build & test (all via Docker)

| Command | What it does |
|---------|--------------|
| `make build` | Compile + package the JAR in a Maven container |
| `make test` | Unit tests (Surefire) in a Maven container |
| `make it-test` | Integration tests (Failsafe + Testcontainers) |
| `make verify` | Unit + integration + Spotless check |
| `make lint` / `make lint-check` | Apply / verify Spotless (Palantir Java Format) |
| `make docker-build` | Build the application image |
| `make up` / `make down` / `make logs` | Manage the full stack |
| `make newman` | Run the Postman collection with Newman in Docker (needs the stack up: `make up`) |

## Security

- **`local` profile (default):** in-memory users + HTTP Basic — `verifier`, `auditor`, `admin`.
- **`aws` profile:** OAuth2 Resource Server (JWT); roles come from the token, enforced with
  `@PreAuthorize`. Issuer is an external IdP (e.g. Amazon Cognito).
- Mocks, `/actuator/health|info|prometheus`, and Swagger UI are open.

## AWS deployment

`infra/` holds Terraform (modules: `network`, `ecr`, `dynamodb`, `config`, `observability`, `ecs`).
`infra/envs/prod` wires them into an ECS Fargate service behind an ALB, with DynamoDB accessed via an
IAM task role. `.github/workflows/deploy.yml` runs `mvn verify` as a gate, then builds/pushes the
image to ECR (tag = commit SHA) and performs a rolling ECS deploy.

The third-party provider is treated as an **external API**: it is not part of this IaC. In AWS,
`verification-service` reaches it through the `third_party_base_url` variable, injected into the task
as `APP_THIRD_PARTY_BASE_URL`. (Locally, `make up` still runs the bundled mock provider over the
Docker network.)

```bash
cd infra/envs/prod
terraform init
terraform plan \
  -var oauth2_issuer_uri=<issuer> \
  -var third_party_base_url=<provider-url>   # requires AWS credentials
```

## Project layout

```
src/main/java/com/incode/verification
  api/            REST controllers (backend, mocks, query) + error handling
  orchestration/  VerificationService (fallback, selection, persistence)
  thirdparty/     ThirdPartyClient port + FREE/PREMIUM RestClient adapters
  mock/           CompanyDataStore (CIN substring filter) + FailureSimulator (503)
  persistence/    VerificationRepository port + DynamoDB Enhanced Client impl
  domain/         Company, Source, VerificationResult (sealed), VerificationRecord
  security/       Spring Security (Basic local / JWT aws), RBAC
  metrics/        Micrometer custom metrics
  config/         RestClient, DynamoDB, OpenAPI, properties
src/main/resources/data/   seed JSON (FREE snake_case, PREMIUM camelCase)
docker/                    Prometheus, Grafana, Loki, Promtail configs
infra/                     Terraform (modules + prod env)
```

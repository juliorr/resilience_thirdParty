# Verification Service

A backend service that verifies companies against two third-party providers and persists every
verification for audit. **FREE** and **PREMIUM** providers are mocked *inside the same service*
(reading local JSON seed files), so the FREE→PREMIUM fallback and the simulated `503` outages are
**real HTTP behaviour**, not in-memory simulation.

Built with **Java 21 + Spring Boot 3.5 + Maven**, persisted in **DynamoDB**, fully **containerized**
(app, tests, and the whole observability stack run in Docker), observable with
**Prometheus + Grafana + Loki**, and deployable to **AWS ECS Fargate** via **Terraform + GitHub
Actions**.

> Design rationale and trade-offs live in [`doc/plans/`](doc/plans/) (`00`–`04`).

## Endpoints

| Endpoint | Auth (role) | Purpose |
|----------|-------------|---------|
| `GET /backend-service?verificationId=<guid>&query=<text>` | `VERIFIER` / `ADMIN` | Run a verification, persist it |
| `GET /verifications/{verificationId}` | `AUDITOR` / `ADMIN` | Retrieve a persisted verification |
| `GET /free-third-party?query=<text>` | open | FREE mock (snake_case, ~40% `503`) |
| `GET /premium-third-party?query=<text>` | open | PREMIUM mock (camelCase, ~10% `503`) |
| `GET /actuator/health` `/prometheus` `/metrics` | open | Health & metrics |
| `GET /swagger-ui.html` `/v3/api-docs` | open | OpenAPI docs |

### Orchestration (`/backend-service`)

1. Call **FREE** first. If it returns `503` **or no results**, fall back to **PREMIUM** (single
   attempt, no retry — retrying would mask the fallback).
2. From the chosen source, return the **first `active` company**; never an inactive one.
3. Extra active matches go in `otherResults`.
4. `result` is the matched company, a `NO_RESULTS` indicator, or a `THIRD_PARTIES_DOWN` indicator.

Every call persists `{ verificationId, queryText, timestamp, result, source }`. Validation:
`400` for missing/blank `query`/`verificationId` or a non-GUID id; `409` if the `verificationId`
already exists (idempotency key, single use); `404` on unknown retrieval.

## Quick start (≈2 minutes)

Requires **Docker** only (no host JDK/Maven needed).

```bash
make up                 # build the image + start app, DynamoDB, Prometheus, Grafana, Loki, Promtail
```

- App: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Grafana: <http://localhost:3000> (anonymous admin)
- Prometheus: <http://localhost:9090>

> **Host port already in use?** Override any host port without touching the file, e.g.
> `APP_HOST_PORT=18080 GRAFANA_HOST_PORT=13000 make up`. Container ports are unchanged.

### Try it

```bash
# Local users (HTTP Basic): verifier/verifier-pass, auditor/auditor-pass, admin/admin-pass
ID=$(uuidgen)

# Run a verification (VERIFIER). Repeat a few times to observe FREE->PREMIUM fallback.
curl -u verifier:verifier-pass "http://localhost:8080/backend-service?verificationId=$ID&query=CJ"

# Retrieve it (AUDITOR) — shows the persisted source (FREE or PREMIUM)
curl -u auditor:auditor-pass "http://localhost:8080/verifications/$ID"

# Duplicate id -> 409 ; bad guid / blank query -> 400 ; unknown id -> 404
```

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

Tests run **two ways** (spec requirement): (a) inside the build via `mvn verify`, and
(b) standalone via `make test` / `make it-test` / `make verify`.

### Integration tests and the Docker socket

Integration tests use **Testcontainers** to launch DynamoDB Local. Because they also run inside a
container, `make it-test`/`make verify` start a **`docker:dind` sidecar** and point Testcontainers
at it over TCP (`DOCKER_HOST=tcp://docker:2375`, `TESTCONTAINERS_HOST_OVERRIDE=docker`). This avoids
the Docker Desktop for macOS limitation where bind-mounting the host socket into a container does not
expose the Engine API. On Linux/CI, `mvn verify` against the native socket works directly; for
**Podman**, point `DOCKER_HOST` at the Podman socket and set `TESTCONTAINERS_RYUK_DISABLED=true`.

## Security

- **`local` profile (default):** in-memory users + HTTP Basic — `verifier`, `auditor`, `admin`.
- **`aws` profile:** OAuth2 Resource Server (JWT); roles come from the token, enforced with
  `@PreAuthorize`. Issuer is an external IdP (e.g. Amazon Cognito).
- Mocks, `/actuator/health|info|prometheus`, and Swagger UI are open.

## AWS deployment

`infra/` holds Terraform (modules: `network`, `ecr`, `dynamodb`, `config`, `observability`, `ecs`).
`infra/envs/prod` wires them into an ECS Fargate service behind an ALB, with DynamoDB accessed via an
IAM task role. `.github/workflows/deploy.yml` runs `mvn verify` as a gate, then builds/pushes the
image to ECR (tag = commit SHA) and performs a rolling ECS deploy. See
[`doc/plans/03-aws-deployment.md`](doc/plans/03-aws-deployment.md).

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

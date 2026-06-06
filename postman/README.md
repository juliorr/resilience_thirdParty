# Postman test cases

Ready-to-run Postman collection for the verification service, built from the seed data in
`src/main/resources/data/free_service_companies-1.json` and
`premium_service_companies-1.json`.

## Files

- `incode-verification.postman_collection.json` — the collection (Postman schema v2.1).
- `incode-local.postman_environment.json` — environment with `baseUrl` (verification-service,
  `http://localhost:8080`), `thirdPartyUrl` (third-party-service, `http://localhost:8081`), and the
  local Basic-auth credentials (`verifier` / `auditor` / `admin`, password `<role>-pass`).

## Prerequisites

The service was split in two, so the collection targets two ports, both started by `make up`:

- `http://localhost:8080` — verification-service (`/backend-service`, `/verifications/{id}`, Basic auth).
- `http://localhost:8081` — third-party-service (`/free-third-party`, `/premium-third-party`, public mocks).

```bash
make up
```

## Run in the Postman app

1. **Import** both JSON files (Import button → drag both in).
2. Select the **Incode Verification - Local** environment (top-right selector).
3. Use **Run collection** (Collection Runner) so the *Create → retrieve → duplicate (flow)*
   folder executes its requests in order.

## Run headless with Newman

```bash
newman run postman/incode-verification.postman_collection.json \
  -e postman/incode-local.postman_environment.json
```

## What the collection covers

| Folder | Purpose |
|--------|---------|
| FREE third-party | `GET /free-third-party` (`:8081`) — snake_case payload, public, asserts field names |
| PREMIUM third-party | `GET /premium-third-party` (`:8081`) — camelCase payload (incl. `companyFullAddress`), shows PREMIUM is the superset |
| Backend service (happy paths) | `GET /backend-service` — match, FREE→PREMIUM fallback, `otherResults`, NO_RESULTS |
| Create → retrieve → duplicate (flow) | create a verification, fetch it by id, then prove a reused id returns 409 |
| Auth & validation errors | 401 (no creds), 403 (wrong role), 404 (unknown id), 400 (invalid GUID / missing query) |
| 503 & fallback (deep) | explicit coverage of the simulated 503s and the FREE→PREMIUM fallback (see below) |

## 503 & fallback (deep) folder

The third-party 503s are random (FREE ~40%, PREMIUM ~10%) and cannot be forced from outside, and
`/backend-service` never returns a 503 (the orchestrator absorbs it) nor does its response carry
`source`. So this folder proves the behaviour in three ways:

| Request(s) | What it proves | Determinism |
|------------|----------------|-------------|
| `A1` + `A2` | `LDL93LOZ` lives only in PREMIUM, so when `/backend-service` returns a match, retrieving the record shows `source = PREMIUM` → the FREE→PREMIUM fallback fired. | Deterministic — runs in any mode |
| `B1` | Calls `/free-third-party` repeatedly until it observes a real **503** and validates the problem body. | Statistical loop |
| `C1` + `C2` | Repeatedly verifies `CJQUNXGW` (present in **both** files) until the persisted `source = PREMIUM`, which can only happen after a **FREE 503** → proves the 503-triggered fallback. | Statistical loop |

> **Important:** blocks `B` and `C` self-loop via `pm.execution.setNextRequest`, which only runs in
> the **Collection Runner** or **Newman** — not with a single *Send*. The loop bound is the
> `loopMax` collection variable (default `25`). With FREE at 40% the odds of missing a 503 in 25
> calls are ~3e-6; if you lower `app.failure.free-probability` in `application.yml`, raise `loopMax`
> accordingly. Block `A` is deterministic and runs in any mode.

## Expected flakiness (by design)

- The **direct** mock endpoints simulate outages: `/free-third-party` returns **503 ~40%** of
  calls and `/premium-third-party` **~10%**. Those tests assert `200 or 503` and only validate
  the body on `200`.
- `/backend-service` **never** returns 503 — the orchestrator absorbs third-party outages. In
  the rare case where both are down on the same call, `result.status` is `THIRD_PARTIES_DOWN`,
  which the assertions tolerate.

## Test queries and why they were chosen

| `query` | Outcome |
|---------|---------|
| `CJQUNXGW` | active in **both** files → match regardless of source |
| `LDL93LOZ` | exists **only** in PREMIUM (active) → forces the FREE→PREMIUM fallback |
| `Q` | matches several active CINs in FREE → first match + non-empty `otherResults` |
| `0ANW1LCD` | present in both but inactive → `NO_RESULTS` |
| `ZZZZZ` | matches nothing → `NO_RESULTS` / empty arrays |
| `FI7` | FREE returns one (`FI75L0O9`), PREMIUM returns two (`FI75L0O9`, `FI7RM46P`) — superset demo |

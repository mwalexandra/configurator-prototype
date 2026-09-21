# Deployment Guide

## 1. Overview

This repository contains a two-tier prototype for SAP Variant Configuration and Pricing (CPS):
- Angular frontend widget in `widget-angular`
- Java Spring Boot API service in `api-service-java`

The backend acts as an adapter between the widget and SAP CPS Runtime APIs.

Request flow:

```
Browser
  -> Angular Widget (widget-angular)
  -> Java API Service (api-service-java)
  -> SAP CPS Runtime (cpservices-product-configuration)
```

## 2. Branches Overview

| Branch | Purpose | SAP CPS Source | Auth Method | Test Product |
|---|---|---|---|---|
| `main` | Quick smoke test without company tenant access | SAP API Business Hub Sandbox | API Key header | `CPS_BURGER` |
| `curaflow-daten` | Realistic test of complex business rules | Curaflow test tenant | OAuth 2.0 Client Credentials via UAA | Curaflow medical product configuration |

Why two branches:
- `main` is optimized for fast onboarding and public sandbox access.
- `curaflow-daten` is optimized for realistic complexity and enterprise-like data.
- Both branches are intentionally kept separate and are not merged.

## 3. Prerequisites

Common prerequisites (both branches):
- Git
- Java 17
- Maven Wrapper (`./mvnw` in backend)
- Node.js compatible with Angular 22 toolchain
- npm 10.x (repo uses `npm@10.9.8`)

Branch-specific prerequisites:

### `main`
- SAP API Business Hub account
- Access to CPS sandbox API product
- API key for sandbox access

### `curaflow-daten`
- Access to Curaflow test tenant
- OAuth client credentials (`client_id`, `client_secret`)
- UAA URL for token retrieval
- CPS runtime base URL for tenant
- Optional verification key from tenant onboarding material (not used directly by current backend code)

## 4. Repository Structure

- `api-service-java`: Spring Boot backend, CPS integration, REST endpoints
- `widget-angular`: Angular widget frontend
- `mta.yaml`: SAP MTA descriptor
- `cps-responses`: sample API payloads and response snapshots (if present locally)

## 5. Setup: `main` (Sandbox/Burger)

1. Checkout branch:
   - `git checkout main`
2. Backend configuration:
   - Copy `api-service-java/src/main/resources/application.properties.example` to `api-service-java/src/main/resources/application.properties` (or set equivalent values externally)
   - Fill placeholders for sandbox base URL and API key
3. Start backend:
   - `cd api-service-java`
   - `./mvnw spring-boot:run`
   - Verify: `http://localhost:8080/health` returns `API Service is running`
4. Frontend configuration:
   - Ensure `widget-angular/proxy.conf.json` points to your backend target
   - `cd ../widget-angular`
   - `npm install`
   - `npm start`
5. Verify frontend:
   - Open `http://localhost:4200`
   - Trigger create configuration flow for `CPS_BURGER`

## 6. Setup: `curaflow-daten`

1. Checkout branch:
   - `git checkout curaflow-daten`
2. Backend configuration:
   - Use `api-service-java/src/main/resources/application.properties.example` from `curaflow-daten` branch
   - Fill OAuth/UAA placeholders (base URL, UAA URL, client ID, client secret)
3. Start backend:
   - `cd api-service-java`
   - `./mvnw spring-boot:run`
   - Verify: `http://localhost:8080/health`
4. Frontend configuration:
   - `cd ../widget-angular`
   - `npm install`
   - Review `proxy.conf.json` target
   - `npm start`
5. Verify frontend:
   - Open `http://localhost:4200`
   - Run create/resume flow with Curaflow product and KB values used by your tenant

Credential sourcing for `curaflow-daten`:
- Obtain tenant credentials through your internal onboarding process (platform owner or integration admin).
- Do not store real secrets in versioned files.

## 7. Configuration Reference

### `main` branch configuration keys

| Key | Layer | Purpose | Required | Example |
|---|---|---|---|---|
| `sap.cps.base-url` | Backend | CPS sandbox base URL | Yes | `https://<sap-business-hub-cps-host>` |
| `sap.cps.api-key` | Backend | Sandbox API key sent as `APIKey` header | Yes | `<SAP_API_KEY>` |
| `app.configurations-db.path` | Backend | Local snapshot storage directory | Optional (default in repo) | `configurations-db` |
| `production` | Frontend env | Angular production mode flag | Yes | `false` |
| `apiUrl` | Frontend env | Base path used by API service in Angular | Yes | `/api` |
| `proxy./api.target` | Frontend proxy | Backend target for local dev proxy | Yes for dev | `http://localhost:8080` |
| `proxy./api.secure` | Frontend proxy | TLS cert validation for proxy target | Optional | `false` |
| `proxy./api.changeOrigin` | Frontend proxy | Host header rewrite for proxy | Optional | `true` |
| `proxy./api.logLevel` | Frontend proxy | Proxy logging level | Optional | `debug` |
| `productId` (request payload) | API payload | Product to configure | Yes for create | `CPS_BURGER` |
| `kbId` (request payload) | API payload | Knowledge base ID/version token | Yes for create | `<BURGER_KB_ID>` |

### `curaflow-daten` branch configuration keys

| Key | Layer | Purpose | Required | Example |
|---|---|---|---|---|
| `sap.cps.base-url` | Backend | CPS tenant runtime base URL | Yes | `https://<cps-tenant-runtime-host>` |
| `sap.cps.uaa-url` | Backend | UAA base URL for OAuth token | Yes | `https://<tenant-uaa-host>` |
| `sap.cps.client-id` | Backend | OAuth client ID | Yes | `<CPS_CLIENT_ID>` |
| `sap.cps.client-secret` | Backend | OAuth client secret | Yes | `<CPS_CLIENT_SECRET>` |
| `app.configurations-db.path` | Backend | Local snapshot storage directory | Optional (default in repo) | `configurations-db` |
| `production` | Frontend env | Angular production mode flag | Yes | `false` |
| `apiUrl` | Frontend env | Base path used by API service in Angular | Yes | `/api` |
| `proxy./api.target` | Frontend proxy | Backend target for local dev proxy | Yes for dev | `http://localhost:8080` |
| `productId` (request payload) | API payload | Curaflow product ID | Yes for create | `<CURAFLOW_PRODUCT_ID>` |
| `kbId` (request payload) | API payload | Curaflow KB ID | Yes for create | `<CURAFLOW_KB_ID>` |
| `verificationKey` | Tenant ops metadata | Tenant validation/onboarding metadata | Optional (not used by current backend code) | `<CURAFLOW_VERIFICATION_KEY>` |

## 8. Verifying the Setup End-to-End

### `main` minimal test flow

1. Start backend and frontend.
2. Create configuration:

```bash
curl -X POST http://localhost:8080/api/configurations \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "CPS_BURGER",
    "kbId": "<BURGER_KB_ID>"
  }'
```

3. Extract `configurationId` from response.
4. Patch one characteristic:

```bash
curl -X PATCH http://localhost:8080/api/configurations/<CONFIGURATION_ID> \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "1",
    "characteristicId": "<CHAR_ID>",
    "value": "<VALUE>"
  }'
```

5. Complete configuration:

```bash
curl -X POST http://localhost:8080/api/configurations/<CONFIGURATION_ID>/complete
```

### `curaflow-daten` minimal test flow

1. Start backend and frontend.
2. Create configuration with tenant product/KB values:

```bash
curl -X POST http://localhost:8080/api/configurations \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "<CURAFLOW_PRODUCT_ID>",
    "kbId": "<CURAFLOW_KB_ID>"
  }'
```

3. Change one characteristic via PATCH.
4. Complete via `/complete` endpoint.

## 9. Known Limitations

Cross-branch limitations:
- Dimension fields are not fully supported in this prototype because required validation behavior is not available from the test-tenant integration layer.

`curaflow-daten` specific limitations:
- External-configuration import may create unresolved conflicts for complex characteristic combinations, which can block successful completion.
- Rare edge case in resume flow: inconsistent `readOnly` status may appear after restoring complex configurations.

## 10. Troubleshooting

- `net::ERR_SOCKET_NOT_CONNECTED`
  - Cause: Wrong frontend proxy target or backend is not running.
  - Fix: Verify backend process and `proxy.conf.json` target.

- `404` on configuration fetch/patch/complete
  - Cause: stale or wrong `configurationId`.
  - Fix: create a fresh configuration and retry with returned ID.

- `409` on `completeConfiguration`
  - Cause: unresolved inconsistencies in configuration state.
  - Fix: re-open configuration, resolve conflicts, retry complete.

- `401` / `403` auth errors
  - In `main`: usually invalid or missing sandbox API key.
  - In `curaflow-daten`: usually expired/wrong OAuth credentials, wrong UAA URL, or missing tenant authorization.

## Branch Switching Notes

To switch safely between profiles:

1. `git status` must be clean before switching.
2. If needed, stash local changes (`git stash -u`) before checkout.
3. Re-apply only branch-appropriate local config files after switch.

## Security Notes

- Never commit real API keys, client secrets, or internal tenant URLs.
- Use local non-versioned files for real credentials.
- Keep only placeholder values in `*.example` files.

# oauth
Serviço oauth - constrsw 2026/2

------ Grupo 07 ------
Membros: Gabriel Hoppe, Juliano Chies, Leonardo Gemin, William Klein

---

## Stack

NestJS 11 + TypeScript, npm. The service is a REST gateway in front of Keycloak
(realm `constrsw`, client `oauth`).

## Environment contract

This service reads its configuration **only from the process environment**,
which the professor's root `docker-compose.yml` injects into the container.

`ConfigModule` is loaded with `ignoreEnvFile: true` on purpose — this service
must never grow its own `.env`. The root `.env` in the `base` repo is
professor-owned and is the single source of these values.

> Never commit secret **values** — not here, not in code, not in tests.
> Only the variable **names** below are documented.

### Required — the service refuses to boot without them

| Variable | Purpose |
| --- | --- |
| `KEYCLOAK_SERVER_URL` | Keycloak base URL, e.g. the internal compose hostname |
| `KEYCLOAK_CLIENT_SECRET` | Secret of the confidential client `oauth` |

A missing or blank value throws `MissingEnvironmentVariableError` during
bootstrap, so a misconfigured container dies immediately instead of failing
later on the first Keycloak call.

### Optional — defaults applied when unset

| Variable | Default |
| --- | --- |
| `KEYCLOAK_REALM` | `constrsw` |
| `KEYCLOAK_CLIENT_ID` | `oauth` |
| `OAUTH_INTERNAL_API_PORT` | `3001` |

### Bound when present

`KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD` — used by the Keycloak Admin API
in the user and role stories (Epics 4-5). Not required to boot.

### Not read by this service

`KEYCLOAK_GRANT_TYPE`, `KEYCLOAK_TOKEN_ALGORITHM`, `KEYCLOAK_INTERNAL_*` and
`KEYCLOAK_EXTERNAL_*` exist in the root `.env` but are not injected into this
container. Grant types are fixed by the contract (`password` for login,
`refresh_token` for refresh), not driven by environment.

Do **not** introduce `KEYCLOAK_URL`, `KEYCLOAK_BASE_URL` or `KEYCLOAK_JWKS_URL`.
The professor's names are the ones above.

## URL construction

Keycloak 26 dropped the `/auth` path segment, so the builder **never inserts
one**:

```
{KEYCLOAK_SERVER_URL}/realms/{realm}/protocol/openid-connect/token
{KEYCLOAK_SERVER_URL}/realms/{realm}/protocol/openid-connect/userinfo
{KEYCLOAK_SERVER_URL}/admin/realms/{realm}
```

If `KEYCLOAK_SERVER_URL` already ends in `/auth` (an older Keycloak), that
prefix is part of the base and is preserved as-is. Trailing slashes are stripped
so joining never produces `//realms`.

Get these URLs from `KeycloakSettingsService` — `tokenUrl`, `userInfoUrl`,
`adminRealmUrl`, plus `realmPath()` / `adminPath()` for anything else. **Do not
read `process.env` in feature modules**; that is what makes the rules above
enforceable in one place.

```ts
constructor(private readonly keycloak: KeycloakSettingsService) {}

// this.keycloak.tokenUrl
// this.keycloak.adminPath('users')
```

## Running locally

Dependencies:

```bash
npm install
```

Tests:

```bash
npm test
```

The required variables must be present in your shell. From the `base` repo root
you can reuse the professor's file for a local run:

```bash
set -a && . ../../.env && set +a && npm run start
```

Do not create an `.env` inside this service, and do not add an `.env.example` —
`.gitignore` blocks the former, and the latter would become an invented group
deliverable.

Docker image, `GET /health` and running the full compose stack are covered by
stories 1.2 and 1.3.

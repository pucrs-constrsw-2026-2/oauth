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

## `POST /login`

Exchanges username/password for Keycloak tokens (Story 2.1, CAP-1). No
`Authorization` header required.

- **Body**: `multipart/form-data` **or** `application/x-www-form-urlencoded` —
  `username`, `password`. Any other content-type is rejected. The T1 brief's
  form also lists `client_id` and `grant_type`; if the caller sends them they
  are **accepted and silently ignored** — the client credentials sent to
  Keycloak always come from `KEYCLOAK_CLIENT_ID` / `KEYCLOAK_CLIENT_SECRET`,
  never from the request.
- **Success**: `200` (not `201` — nothing is created) with JSON:

  ```json
  {
    "token_type": "Bearer",
    "access_token": "...",
    "expires_in": 300,
    "refresh_token": "...",
    "referesh_expires_in": 1800
  }
  ```

  `referesh_expires_in` keeps the brief's spelling on purpose — it is mapped
  from Keycloak's `refresh_expires_in`.
- **Errors**: `400` when `username`/`password` is missing/blank or the
  content-type is unsupported (local `OA-400`, checked before any Keycloak
  call); `401` when Keycloak rejects the credentials (`error_code` relays
  Keycloak's `error`, typically `invalid_grant`). Both use the OA envelope
  above.

## `POST /refresh`

Trades a valid `refresh_token` for a new token pair without re-entering the
password (Story 2.2, CAP-7). No `Authorization` header required — the refresh
token itself is the credential.

- **Body**: `multipart/form-data` **or** `application/x-www-form-urlencoded` —
  `refresh_token` only.
- **Success**: `200` with the same field set as `/login` (`token_type`,
  `access_token`, `expires_in`, `refresh_token`, `referesh_expires_in` when
  Keycloak returns a refresh expiry).
- **Errors**: `400` when `refresh_token` is missing/blank or the content-type
  is unsupported; `401` when the refresh token is invalid or expired
  (`error_code` relays Keycloak's `error`, typically `invalid_grant`).

## Authentication on protected routes

Routes under `/users` and `/roles` require the caller's Keycloak access token:

```
Authorization: Bearer <access_token>
```

`BearerAuthGuard` runs before the handler. Apply it with `@UseGuards`; the
resolved caller is attached to the request:

```ts
@UseGuards(BearerAuthGuard)
@Controller('users')
export class UsersController {
  @Get()
  list(@Req() req: AuthenticatedRequest) {
    req.user; // { sub, email, preferredUsername, name, raw }
  }
}
```

`CommonModule` is `@Global()`, so importing it once in `AppModule` is enough.

### How the guard verifies a token

It calls Keycloak's UserInfo endpoint rather than validating the JWT signature
locally. Keycloak therefore stays the authority on whether a token is still
usable, so a logout or a disabled account takes effect immediately instead of
lingering until the token expires. The cost is one upstream call per request,
which this service accepts.

No JWKS variable is introduced — see the environment contract above.

### 401 vs 403

| Status | Meaning | Decided by |
| --- | --- | --- |
| `401` | No token, malformed header, or Keycloak does not accept the token | The guard |
| `403` | The caller is known but not allowed to perform the operation | Keycloak |
| `503` | The token could not be verified because Keycloak is unreachable | The guard |

**The guard never decides permissions.** Any caller with a token Keycloak
accepts passes it, even with no roles at all. Authorization is answered by
Keycloak when the route calls the Admin API: a caller lacking the required
`realm-management` rights gets a `403` from Keycloak, which the route relays.

This is deliberate. Holding a local role-to-operation table here would
duplicate the realm's own configuration and drift from it — the B.2 permission
matrix in the brief already differs from what `constrsw.json` actually
configures. Keycloak is the single source of truth.

`503` exists so that an outage is not reported as an authentication failure: a
caller with a perfectly valid token must not be told to sign in again because a
dependency is down.

### Error format

The guard throws the standard Nest exceptions. Once the uniform error envelope
(Story 3.1) registers its exception filter, those responses are formatted into
`{ error_code, error_description, error_source, error_stack }` with no change
required here.

## Error format (uniform envelope)

Every error response — from every module — uses the same JSON body:

```json
{
  "error_code": "OA-400",
  "error_description": "username is required",
  "error_source": "OAuthAPI",
  "error_stack": [{ "field": "username", "reason": "missing" }]
}
```

| Field | Rule |
| --- | --- |
| `error_code` | Relays Keycloak's `error` field when the failure came from a Keycloak call; otherwise a local `OA-xxx` code (below) |
| `error_description` | Group-provided human message |
| `error_source` | Always `OAuthAPI` |
| `error_stack` | **Array of objects**, chaining causes down to the root cause — never a string or an array of strings |

### Local `OA-xxx` code family

Used when the failure is local (bad structure, missing/invalid auth) and Keycloak was not called or returned no code. Epics 4-6 **must reuse this table** — do not invent a new prefix (`ERR_USER_*`, etc.):

| Code | HTTP status | Meaning |
| --- | --- | --- |
| `OA-400` | 400 | Bad request structure / validation failure |
| `OA-401` | 401 | Missing or invalid authentication (local, no Keycloak code to relay) |
| `OA-403` | 403 | Forbidden (local) |
| `OA-404` | 404 | Entity not found |
| `OA-409` | 409 | Conflict (e.g. duplicate username) |
| `OA-500` | 500 | Unexpected internal error |

### How it's wired

- `src/errors/oa-error.mapper.ts` — `OaException` + `OaErrorMapper` factory (`badRequest`, `unauthorized`, `forbidden`, `notFound`, `conflict`, `fromKeycloak`). Route/service code throws these instead of building JSON by hand.
- `src/errors/oa-exception.filter.ts` — global Nest exception filter (`app.useGlobalFilters` in `main.ts`). Formats **any** thrown value — `OaException`, a standard Nest `HttpException`, or an unexpected error — into the envelope above. No route can fall back to Nest's default `{ statusCode, message }` body.

## Authorization model (client `oauth`)

Stories 6.1-6.4 are **verify-first**: the professor's realm import
(`infrastructure/dev.local/services/keycloak/constrsw.json`) already ships the
full B.2 authorization model for client `oauth`. This group does **not** own
or edit that file — it verifies what the import contains and would only
gap-fill genuinely missing objects via the running Admin Console or Admin API.

> **Verification method used here:** static inspection of `constrsw.json` as
> committed to the `base` repo (the same file `start-dev --import-realm`
> loads). This confirms what *will* be imported. Docker/Keycloak were not
> running in this environment, so **live** Admin Console re-confirmation after
> an actual `docker compose up` + volume import is still recommended before
> treating 6.1-6.4 as fully closed — see the caveat under each story below.

### Story 6.1 — client roles

Client roles on `oauth`, confirmed present in `constrsw.json`:

| Role | Present |
| --- | --- |
| `administrator` | ✅ |
| `coordinator` | ✅ |
| `professor` | ✅ |
| `student` | ✅ |

These are the canonical **English** B.2 names — never `funcionario` /
`coordenador`. No gap to fill; no realm JSON change needed. (The client also
defines `uma_protection`, a Keycloak-internal role for its own Authorization
Services client — not part of the B.2 set, left as-is.)

Story 5.6's logical-delete representation for roles created via this API is
separate from these four client roles; B.2 policy binding always uses the
canonical **active** role names above, never an inactive/soft-deleted form.

**Still pending:** confirm the same four roles appear under **Client roles**
for `oauth` in the running Admin Console (`:8081`) after the volume import,
since a stale `constrsw-keycloak-data` volume would not pick up JSON changes.

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

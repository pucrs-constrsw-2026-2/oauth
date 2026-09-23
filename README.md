# oauth

Serviço oauth — ConstrSW 2026/2 — Grupo 04 (students)

API REST em **Clean Architecture** sobre Java 21 / Spring Boot 3.3 que encapsula
a REST API do Keycloak para autenticação (`POST /login`) e gestão de usuários
(`CRUD /users`). Sobe junto com o Keycloak via `docker compose up` na raiz do
repositório `base`.

---

## URLs úteis (com o compose no ar)

| Recurso                | URL                                         |
| ---------------------- | ------------------------------------------- |
| Swagger UI             | http://localhost:8181/swagger-ui/index.html |
| OpenAPI JSON           | http://localhost:8181/v3/api-docs           |
| Healthcheck            | http://localhost:8181/actuator/health       |
| Prometheus metrics     | http://localhost:8181/actuator/prometheus   |
| Base API               | http://localhost:8181                       |
| Keycloak Admin Console | http://localhost:8081                       |

Porta externa da API = `OAUTH_EXTERNAL_API_PORT` no `.env` da raiz (padrão `8181`).

---

## Escopo entregue (Users + Auth)

Todas as rotas de **USERS** e **LOGIN** exigidas pelo enunciado estão
implementadas e documentadas no Swagger. A suíte automatizada cobre o
contrato HTTP dos controllers, os casos de uso e os gateways contra uma API
Keycloak simulada; a validação end-to-end com o Keycloak real é feita pelo
`docker compose`.

| Rota          | Método                             | Status codes                |
| ------------- | ---------------------------------- | --------------------------- |
| `/login`      | `POST`                             | 201 / 400 / 401             |
| `/users`      | `POST`                             | 201 / 400 / 401 / 403 / 409 |
| `/users`      | `GET` (com `?enabled=true\|false`) | 200 / 400 / 401 / 403       |
| `/users/{id}` | `GET`                              | 200 / 400 / 401 / 403 / 404 |
| `/users/{id}` | `PUT`                              | 200 / 400 / 401 / 403 / 404 |
| `/users/{id}` | `PATCH` (senha)                    | 200 / 400 / 401 / 403 / 404 |
| `/users/{id}` | `DELETE` (soft, `enabled=false`)   | 204 / 400 / 401 / 403 / 404 |

Validações:

- **RFC 5322** para o `username` no `POST` e `PUT` (`EmailValidator`)
- Bearer token obrigatório em todas as rotas exceto as públicas listadas na
  `SecurityConfig` (`/login`, `/swagger-ui/**`, `/v3/api-docs/**`,
  `/actuator/health`)
- `username = e-mail` no Keycloak
- **Exclusão lógica** no `DELETE` (`enabled=false`, sem remover o usuário)

Rotas de **Roles** e **atribuição de roles a users** também estão disponíveis:
`POST/GET/GET-id/PUT/PATCH/DELETE /roles`, `POST /roles/{roleId}/users/{userId}`
e `DELETE /roles/{roleId}/users/{userId}`.

---

## Convenção de payloads

Os DTOs usam **kebab-case** para `first-name` e `last-name` (via
`@JsonProperty`), seguindo o exemplo do enunciado. Exemplo de `POST /users`:

```json
{
  "username": "aluno@pucrs.br",
  "password": "senha123",
  "first-name": "Aluno",
  "last-name": "Teste"
}
```

Se enviar `firstName`/`lastName` em camelCase, o Jackson NÃO faz a
correspondência e esses campos ficam `null` no Keycloak.

---

## Formato padrão de erro

**Todos** os erros da API — validação, token ausente/inválido, `/login` que
falhou, `/users` que deu 4xx, exceção não tratada — respondem no envelope do
enunciado:

```json
{
  "error_code": "400",
  "error_description": "Invalid e-mail (RFC 5322): nao-eh-email",
  "error_source": "OAuthAPI",
  "error_stack": [
    {
      "type": "InvalidEmailException",
      "message": "Invalid e-mail (RFC 5322): nao-eh-email"
    }
  ]
}
```

- `error_code` = valor do HTTP status (`"400"`, `"401"`, `"403"`, `"404"`, `"409"`, `"500"`, `"503"`)
- `error_description` = mensagem do desenvolvedor
- `error_source` = `"OAuthAPI"`
- `error_stack` = array de `{ type, message }` com a origem da falha

---

## Arquitetura — Clean Architecture

O código está organizado em três camadas concêntricas, com a **regra de
dependência apontando sempre pra dentro**: `infrastructure` conhece
`application`, `application` conhece `domain`, e o `domain` não conhece
ninguém (nem Spring, nem HTTP, nem Keycloak).

```
                  ┌─────────────────────────────────────────────┐
                  │           infrastructure/                    │
                  │                                              │
                  │   adapter/in/rest ──► use case (in port)     │
                  │   adapter/out/keycloak ◄─ gateway (out port) │
                  │   config / security / util                   │
                  │            │                                 │
                  │            ▼                                 │
                  │   ┌────────────────────────────┐             │
                  │   │      application/          │             │
                  │   │                            │             │
                  │   │  port/in/  use case ifs    │             │
                  │   │  port/out/ gateway ifs     │             │
                  │   │  usecase/  services @Service│            │
                  │   │            │               │             │
                  │   │            ▼               │             │
                  │   │   ┌────────────────┐       │             │
                  │   │   │   domain/      │       │             │
                  │   │   │                │       │             │
                  │   │   │ model/  User,  │       │             │
                  │   │   │         AuthTokens,    │             │
                  │   │   │         Credentials,   │             │
                  │   │   │         NewUser, UserUpdate         │
                  │   │   │ exception/ InvalidEmail,│           │
                  │   │   │         UserNotFound,  │             │
                  │   │   │         UserAlreadyExists,           │
                  │   │   │         InvalidCredentials,          │
                  │   │   │         AccessDenied, ...            │
                  │   │   └────────────────┘       │             │
                  │   └────────────────────────────┘             │
                  └─────────────────────────────────────────────┘
```

### Camadas

**`domain/`** — Puro Java, zero Spring, zero anotações Jakarta/HTTP.

- `model/` — entidades e value objects (`User`, `AuthTokens`, `Credentials`,
  `NewUser`, `UserUpdate`)
- `exception/` — hierarquia de `DomainException` (`InvalidEmailException`,
  `InvalidCredentialsException`, `UserNotFoundException`,
  `UserAlreadyExistsException`, `AccessDeniedException`,
  `AuthorizationRequiredException`, `InvalidInputException`,
  `IdentityProviderUnavailableException`)

**`application/`** — Casos de uso + ports (interfaces).

- `port/in/` — interfaces dos casos de uso (`LoginUseCase`,
  `CreateUserUseCase`, `ListUsersUseCase`, `GetUserUseCase`,
  `UpdateUserUseCase`, `UpdatePasswordUseCase`, `DisableUserUseCase`)
- `port/out/` — interfaces para infraestrutura externa (`AuthGateway`,
  `UserGateway`)
- `usecase/` — implementações (`LoginService`, `CreateUserService`,
  `ListUsersService`, `GetUserService`, `UpdateUserService`,
  `UpdatePasswordService`, `DisableUserService`) — cada uma implementa a
  interface do `port/in/` correspondente e depende só de ports (via injeção
  de construtor)

**`infrastructure/`** — Adapters + configuração de framework.

- `config/` — `KeycloakProperties` (`@ConfigurationProperties`),
  `HttpClientConfig` (Apache HttpClient 5 + RestTemplate no-op error handler),
  `SecurityConfig` (Spring Security como Resource Server JWT),
  `OpenApiConfig` (SpringDoc)
- `security/` — `CustomAuthenticationEntryPoint` (401) e
  `CustomAccessDeniedHandler` (403), ambos serializam o envelope padrão
- `util/` — `EmailValidator` (regex RFC 5322)
- `adapter/in/rest/` — **entrada HTTP**: `AuthRestController` (POST /login),
  `UserRestController` (CRUD /users), `ApiExceptionHandler`
  (`@RestControllerAdvice` que traduz domain exceptions em HTTP status)
- `adapter/in/rest/dto/` — DTOs HTTP (`LoginResponseDto`,
  `UserCreateRequestDto`, `UserUpdateRequestDto`, `UserResponseDto`,
  `PasswordUpdateRequestDto`, `ErrorResponseDto`, `ErrorStackEntryDto`),
  cada um com método `.toDomain()` / `.fromDomain()`
- `adapter/out/keycloak/` — implementações concretas dos ports out:
  `KeycloakAuthGateway` (implementa `AuthGateway`) e `KeycloakUserGateway`
  (implementa `UserGateway`). São os únicos pontos do código que conhecem
  Keycloak.

### Regra de dependência (Clean Architecture)

- `domain` **não importa NADA** de fora
- `application` importa **só de `domain`** (nunca de `infrastructure`)
- `infrastructure` importa de `domain` **e** de `application`
- Anotações Spring (`@Service`, `@RestController`, `@Component`,
  `@Configuration`) só aparecem em `application/usecase/` (para o Spring
  registrar as implementações dos ports in) e em `infrastructure/`

### Fluxo de uma request `POST /users`

```
┌─────────────┐  1. HTTP POST /users
│   Cliente   │──────────────────────────────────┐
└─────────────┘                                  ▼
                                    ┌──────────────────────────┐
                                    │  Spring Security         │
                                    │  (valida Bearer JWT)     │
                                    └────────────┬─────────────┘
                                                 │ 2. autenticado
                                                 ▼
                                    ┌──────────────────────────┐
                                    │  UserRestController      │  (infrastructure/adapter/in)
                                    │  request → NewUser       │
                                    └────────────┬─────────────┘
                                                 │ 3. execute(bearer, newUser)
                                                 ▼
                                    ┌──────────────────────────┐
                                    │  CreateUserService       │  (application/usecase, impl CreateUserUseCase)
                                    │  valida email RFC 5322   │
                                    └────────────┬─────────────┘
                                                 │ 4. userGateway.create(...)
                                                 ▼
                                    ┌──────────────────────────┐
                                    │  UserGateway (interface) │  (application/port/out)
                                    └────────────┬─────────────┘
                                                 │ Spring injeta a impl
                                                 ▼
                                    ┌──────────────────────────┐
                                    │  KeycloakUserGateway     │  (infrastructure/adapter/out)
                                    │  HTTP POST admin/realms/ │
                                    │       constrsw/users     │
                                    └────────────┬─────────────┘
                                                 │
                                                 ▼
                                    ┌──────────────────────────┐
                                    │       Keycloak           │
                                    │   Admin REST API         │
                                    └──────────────────────────┘
```

O **domain** (`User`, `NewUser`) atravessa todas as camadas sem depender de
nada externo. Se amanhã quiséssemos trocar Keycloak por Auth0/Okta, o único
código que precisaria mudar seria `infrastructure/adapter/out/keycloak/` —
basta criar `Auth0UserGateway implements UserGateway` e trocar a bean; nada
em `application/` ou `domain/` seria tocado.

### Por que Clean Architecture aqui?

- **Testabilidade**: um `CreateUserService` pode ser testado com um mock de
  `UserGateway`, sem subir Spring nem Keycloak
- **Trocabilidade**: Keycloak é detalhe de infraestrutura, não regra de
  negócio — poderia ser substituído por outro Identity Provider trocando só
  o gateway
- **Legibilidade**: o que a API faz (use cases) fica claro em
  `application/usecase/`, sem se misturar com como HTTP funciona ou como o
  Keycloak responde

---

## Observabilidade OpenTelemetry

O Spring Boot instrumenta automaticamente as requisições HTTP e o Spring
Security com Micrometer Observation. O projeto usa o bridge
`micrometer-tracing-bridge-otel` para gerar traces e o exporter OTLP para
enviá-los a um collector OpenTelemetry.

Métricas no formato Prometheus ficam disponíveis em:

```text
GET /actuator/prometheus
```

Por padrão, o endpoint OTLP de traces é `http://localhost:4318/v1/traces`.
Em Docker ou em outro ambiente, configure:

```text
OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://otel-collector:4318/v1/traces
OTEL_TRACES_SAMPLER_ARG=1.0
```

O endpoint `/actuator/health` continua sendo usado pelo healthcheck do
`docker-compose`; o endpoint Prometheus exige autenticação Bearer, como as
demais rotas protegidas.

---

## Execução

### Junto com o Keycloak (recomendado)

Na raiz do repositório `base`:

```bash
docker compose up -d
```

Verificar:

```bash
docker compose ps                                # keycloak e oauth "healthy"
curl http://localhost:8181/actuator/health       # {"status":"UP"}
```

### Só este serviço

```bash
mvn spring-boot:run
# ou
docker build -t oauth .
docker run -p 3001:3001 \
  -e OAUTH_INTERNAL_API_PORT=3001 \
  -e KEYCLOAK_SERVER_URL=http://host.docker.internal:8080 \
  -e KEYCLOAK_REALM=constrsw \
  -e KEYCLOAK_CLIENT_ID=oauth \
  -e KEYCLOAK_CLIENT_SECRET=<secret> \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=<senha> \
  oauth
```

---

## Variáveis de ambiente

| Variável                  | Descrição                                            | Default                 |
| ------------------------- | ---------------------------------------------------- | ----------------------- |
| `OAUTH_INTERNAL_API_PORT` | Porta interna do Spring Boot                         | `8080`                  |
| `KEYCLOAK_SERVER_URL`     | URL interna do Keycloak (nome do serviço na network) | `http://localhost:8080` |
| `KEYCLOAK_REALM`          | Realm usado pela aplicação                           | `constrsw`              |
| `KEYCLOAK_CLIENT_ID`      | Client confidencial cadastrado no realm              | `oauth`                 |
| `KEYCLOAK_CLIENT_SECRET`  | Secret do client `oauth`                             | _(obrigatório)_         |
| `KEYCLOAK_ADMIN`          | Admin bootstrap do Keycloak (realm `master`)         | `admin`                 |
| `KEYCLOAK_ADMIN_PASSWORD` | Senha do admin bootstrap do Keycloak                 | _(obrigatório)_         |

O `docker-compose.yml` da raiz já injeta todas essas variáveis a partir do
`.env`.

---

## Teste rápido (PowerShell)

```powershell
# 1. Token
$token = (curl.exe -s -X POST http://localhost:8181/login `
    -F "username=admin@pucrs.br" -F "password=12345" | ConvertFrom-Json).access_token

# 2. Listar users
curl.exe -s -H "Authorization: Bearer $token" http://localhost:8181/users

# 3. Criar user
'{"username":"aluno@pucrs.br","password":"senha","first-name":"Aluno","last-name":"Teste"}' `
    | Out-File body.json -Encoding ascii -NoNewline
curl.exe -s -X POST http://localhost:8181/users `
    -H "Authorization: Bearer $token" -H "Content-Type: application/json" `
    --data "@body.json"
```

### Testes automatizados

Na pasta `backend/oauth`:

```bash
mvn test
```

A suíte contém testes unitários dos casos de uso com gateways mockados,
testes de integração `@SpringBootTest`/`MockMvc` dos controllers e testes de
contrato dos gateways com `MockRestServiceServer`, incluindo segurança e o
endpoint Prometheus. Os testes não dependem de um Keycloak rodando.

---

## Nota sobre versão do Keycloak

O enunciado descreve as rotas do Keycloak com prefixo `/auth`
(`{{base-keycloak-url}}/auth/realms/...`). O Keycloak deste projeto é o **v26**
(Quarkus), que **removeu o context-path `/auth`** desde a v17. As URLs
montadas por `KeycloakProperties` refletem essa mudança — `/realms/...` em
vez de `/auth/realms/...`. Não é desvio do enunciado, é ajuste de versão.

---

## Roadmap (fora do escopo desta entrega)

- Testcontainers com Keycloak real para uma suíte opcional de contrato da Admin API
- Collection Postman consolidada
- Tag de release no GitHub + zip para entrega no Moodle

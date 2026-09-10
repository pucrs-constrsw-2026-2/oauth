# oauth

Serviço oauth — ConstrSW 2026/2 — Grupo 04 (students)

API REST em Java/Spring Boot 3.3 (Java 21) que encapsula a REST API do Keycloak
para autenticação e gestão de usuários. Sobe junto com o Keycloak via
`docker compose up` na raiz do repositório `base`.

---

## URLs úteis (com o compose no ar)

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8181/swagger-ui/index.html |
| OpenAPI JSON | http://localhost:8181/v3/api-docs |
| Healthcheck | http://localhost:8181/actuator/health |
| Base API | http://localhost:8181 |
| Keycloak Admin Console | http://localhost:8081 |

Porta externa da API = `OAUTH_EXTERNAL_API_PORT` no `.env` da raiz (padrão `8181`).

---

## Escopo entregue (Users)

Todas as rotas de **USERS** e **LOGIN** exigidas pelo enunciado estão
implementadas, testadas end-to-end contra o Keycloak 26 e documentadas no
Swagger:

| Rota | Método | Status codes |
|---|---|---|
| `/login` | `POST` | 201 / 400 / 401 |
| `/users` | `POST` | 201 / 400 / 401 / 403 / 409 |
| `/users` | `GET` (com `?enabled=true\|false`) | 200 / 400 / 401 / 403 |
| `/users/{id}` | `GET` | 200 / 400 / 401 / 403 / 404 |
| `/users/{id}` | `PUT` | 200 / 400 / 401 / 403 / 404 |
| `/users/{id}` | `PATCH` (senha) | 200 / 400 / 401 / 403 / 404 |
| `/users/{id}` | `DELETE` (soft, `enabled=false`) | 204 / 400 / 401 / 403 / 404 |

Validações:

- **RFC 5322** para o `username` no `POST` e `PUT` (`EmailValidator`)
- Bearer token obrigatório em todas as rotas exceto as públicas listadas na
  `SecurityConfig` (`/login`, `/swagger-ui/**`, `/v3/api-docs/**`,
  `/actuator/health`)
- `username = e-mail` no Keycloak
- **Exclusão lógica** no `DELETE` (`enabled=false`, sem remover o usuário)

Rotas de **Roles** e **atribuição de roles a users** — **não implementadas**
nesta entrega.

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
    { "type": "BadRequestException", "message": "Invalid e-mail (RFC 5322): nao-eh-email" }
  ]
}
```

- `error_code` = valor do HTTP status (`"400"`, `"401"`, `"403"`, `"404"`,
  `"409"`, `"500"`)
- `error_description` = mensagem do desenvolvedor
- `error_source` = `"OAuthAPI"` (ou `"Keycloak"` quando repassamos um erro do
  Keycloak)
- `error_stack` = array de `{ type, message }` com a origem da falha

---

## Arquitetura

Java 21 + Spring Boot 3.3.4, empacotado como fat-JAR e executado num container
Alpine + JRE 21. RestTemplate + Apache HttpClient 5 para consumo do Keycloak.
Spring Security como Resource Server valida o JWT emitido pelo Keycloak em
cada request.

Dois packages Java convivem no mesmo classpath:

```
src/main/java/
├── br/pucrs/constrsw/oauth/                (código do merge — camada de auth/base)
│   ├── OauthApplication.java               (@SpringBootApplication único, scanBasePackages
│   │                                        cobre também com.constrsw.oauth)
│   ├── config/                             (KeycloakProperties, SecurityConfig,
│   │                                        OpenApiConfig, AppConfig com RestTemplate bean)
│   ├── controller/AuthController.java      (POST /login)
│   ├── dto/                                (LoginResponse, ErrorResponse, ErrorStackEntry)
│   ├── exception/                          (OAuthApiException, GlobalExceptionHandler)
│   ├── security/                           (CustomAuthenticationEntryPoint,
│   │                                        CustomAccessDeniedHandler)
│   └── service/                            (KeycloakAuthService, KeycloakAdminService)
│
└── com/constrsw/oauth/                     (código do stash — CRUD de Users)
    ├── OAuthApplication.java               (main() legacy, SEM @SpringBootApplication;
    │                                        preservado como referência)
    ├── config/                             (LegacyKeycloakProperties, RestTemplateConfig,
    │                                        OpenApiConfig sem @Configuration)
    ├── controller/                         (UserController — /users; LoginController legacy
    │                                        sem @RestController pra não colidir com AuthController)
    ├── dto/                                (UserCreateRequest, UserUpdateRequest, UserResponse,
    │                                        PasswordUpdateRequest, LoginResponse, ErrorResponse)
    ├── exception/                          (ApiException + subclasses BadRequest/Conflict/
    │                                        NotFound/Unauthorized/Forbidden/KeycloakUnavailable;
    │                                        LegacyGlobalExceptionHandler com @Order HIGHEST
    │                                        e basePackages="com.constrsw.oauth")
    ├── service/KeycloakService.java        (chama Admin REST API do Keycloak com o Bearer
    │                                        que veio no header, para o CRUD de users)
    └── util/EmailValidator.java            (RFC 5322)
```

### Por que dois packages?

Este submódulo passou por um `merge` da branch base (`br.pucrs.constrsw.oauth`
— login + security + envelope de erro do enunciado) sobre uma implementação
anterior do stash (`com.constrsw.oauth` — CRUD completo de Users). Em vez de
reescrever um deles, os dois convivem:

- `br.pucrs...OauthApplication` é a única `@SpringBootApplication`, com
  `scanBasePackages` para os dois roots.
- Colisões por nome de bean foram resolvidas renomeando `KeycloakProperties` →
  `LegacyKeycloakProperties` e `GlobalExceptionHandler` →
  `LegacyGlobalExceptionHandler` no lado do stash.
- Colisões de mapping (dois `POST /login`) e de bean (dois `OpenAPI`) foram
  resolvidas neutralizando as anotações Spring do lado do stash
  (`@RestController` e `@Configuration` foram removidos das duplicatas).
- O `LegacyGlobalExceptionHandler` tem `@Order(HIGHEST_PRECEDENCE)` e
  `basePackages="com.constrsw.oauth"` para pegar as exceções custom do stash
  antes do handler global do merge, e reutiliza o `ErrorResponse` /
  `ErrorStackEntry` do merge para manter o mesmo formato de envelope em toda
  a API.

### Fluxo de uma request `/users`

```
Client ──> Tomcat ──> Spring Security (valida Bearer JWT contra o Keycloak)
                                    │
                                    ▼
                         UserController (com.constrsw...)
                                    │
                                    ▼
                         KeycloakService (com.constrsw...)
                                    │  RestTemplate + HttpClient5
                                    ▼
                         Keycloak Admin REST API
                         (/admin/realms/constrsw/users...)
```

Nenhuma request para `/users` cria/administra usuário sem o Bearer — o próprio
serviço repassa o `Authorization` recebido para a Admin API do Keycloak, que
decide se o usuário representado pelo token tem permissão (`manage-users` no
realm `constrsw`). O usuário `admin@pucrs.br` já é seedado no realm-export com
essa role.

---

## Execução

### Junto com o Keycloak (recomendado)

Na raiz do repositório `base`:

```bash
docker compose up -d
```

Depois, verifique:

```bash
docker compose ps            # keycloak e oauth devem ficar "healthy"
curl http://localhost:8181/actuator/health   # {"status":"UP"}
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

| Variável | Descrição | Default |
|---|---|---|
| `OAUTH_INTERNAL_API_PORT` | Porta que o Spring Boot escuta dentro do container | `8080` |
| `KEYCLOAK_SERVER_URL` | URL interna do Keycloak (nome do serviço na network) | `http://localhost:8080` |
| `KEYCLOAK_REALM` | Realm usado pela aplicação | `constrsw` |
| `KEYCLOAK_CLIENT_ID` | Client confidencial cadastrado no realm | `oauth` |
| `KEYCLOAK_CLIENT_SECRET` | Secret do client `oauth` | *(obrigatório)* |
| `KEYCLOAK_ADMIN` | Usuário admin bootstrap do Keycloak (realm `master`) | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Senha do admin bootstrap do Keycloak | *(obrigatório)* |

O `docker-compose.yml` da raiz já injeta todas essas variáveis a partir do
`.env`.

---

## Teste rápido (cmd.exe / PowerShell)

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

Passo a passo completo (POST/GET/PUT/PATCH/DELETE + testes de erro) no chat
de desenvolvimento do grupo.

---

## Nota sobre versão do Keycloak

O enunciado descreve as rotas do Keycloak com prefixo `/auth`
(`{{base-keycloak-url}}/auth/realms/...`). O Keycloak deste projeto é o **v26**
(Quarkus), que **removeu o context-path `/auth`** desde a v17. As URLs
montadas por `KeycloakProperties.tokenEndpoint()` e por `KeycloakService`
(admin API) refletem essa mudança — `/realms/...` em vez de `/auth/realms/...`.
Não é desvio do enunciado, é ajuste de versão.

---

## Roadmap (fora do escopo desta entrega)

- Rotas de **Roles** (`POST/GET/GET-id/PUT/PATCH/DELETE /roles`)
- Endpoints para **atribuir/remover role** de um usuário
- Collection Postman consolidada
- Tag de release no GitHub + zip para entrega no Moodle

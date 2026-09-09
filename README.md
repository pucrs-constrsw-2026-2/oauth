# oauth

Serviço oauth - ConstrSW 2026/2 - Grupo 04 (students)

API REST em Java/Spring Boot que encapsula a REST API do Keycloak para autenticação,
gestão de usuários e de roles. Ver o `README.md` do repositório `base` para instruções
completas de execução via `docker compose`.

## Status desta branch (`grupo04`)

Escopo entregue nesta etapa (responsável: configuração + login):

- Estrutura base do projeto Spring Boot (Java 21 / Spring Boot 3.3.4, Maven).
- `Dockerfile` multi-stage (build com Maven, runtime com JRE 21 Alpine).
- `POST /login` — autentica um usuário consumindo o endpoint de token do Keycloak
  (Direct Access Grant / `grant_type=password`) e devolve `token_type`,
  `access_token`, `expires_in`, `refresh_token`, `refresh_expires_in`.
  > **Nota sobre a URL do Keycloak**: o enunciado descreve a rota como
  > `{{base-keycloak-url}}/auth/realms/{{realm}}/protocol/openid-connect/token` (com
  > `/auth`). O Keycloak deste projeto é a v26 (Quarkus), que **não usa mais** o
  > context-path `/auth` desde a v17 — por isso `KeycloakProperties.tokenEndpoint()`
  > monta a URL como `{{base-keycloak-url}}/realms/{{realm}}/protocol/openid-connect/token`
  > (sem `/auth`), validado de ponta a ponta contra o `docker-compose.yml` deste
  > repositório. Isso é uma diferença de versão do Keycloak, não um desvio deliberado
  > do enunciado.
- Autenticação Bearer: todas as demais rotas exigem `Authorization: Bearer <token>`,
  validado como JWT do Keycloak (`spring-boot-starter-oauth2-resource-server`).
- Chamadas administrativas ao Keycloak (para Users/Roles): a service account do client
  `oauth` **não** tem permissão (`manage-users`/`manage-realm`) no realm `constrsw` (ver
  `keycloak/realm-export.json`). Por isso foi criado `KeycloakAdminService`, que
  autentica como o admin bootstrap do Keycloak (`KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD`,
  já injetados no container pelo `docker-compose.yml`) contra o client `admin-cli` do
  realm `master`, cacheia o token e expõe:
  - `adminApiBaseUrl()` → `{KEYCLOAK_SERVER_URL}/admin/realms/{KEYCLOAK_REALM}`
  - `adminAuthHeaders()` → headers prontos (`Authorization: Bearer <admin-token>` +
    `Content-Type: application/json`) para chamar a Admin REST API do Keycloak.
- Tratamento de erros geral: todo erro da API (validação, token ausente/inválido,
  falha ao chamar o Keycloak, exceção não tratada) responde no envelope padrão:

  ```json
  {
    "error_code": "...",
    "error_description": "...",
    "error_source": "...",
    "error_stack": [{ "type": "...", "message": "..." }]
  }
  ```

  Lance `OAuthApiException` (tem factories `badRequest`/`unauthorized`/`forbidden`) a
  partir de qualquer controller/service novo para cair automaticamente no
  `GlobalExceptionHandler` e manter esse contrato.

Pendente (outras pessoas do grupo, ver README da raiz do repo `base`):

- `POST/GET/PUT/PATCH/DELETE /users` (Matheus)
- `POST/GET/PUT/PATCH/DELETE /roles` + atribuição de role a usuário (Guilherme)
- Swagger completo, collection Postman, testes, tag e `.zip` de entrega (Lucas)

Para as duas primeiras: siga o padrão já existente (`controller` fino → `service` que
fala com o Keycloak → `dto`s dedicados), reaproveitando `KeycloakAdminService` para as
chamadas à Admin REST API. O regex de validação de e-mail do enunciado (`POST /users`)
ainda não foi implementado.

## Rodando só este serviço (fora do compose da raiz)

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
  -e KEYCLOAK_ADMIN_PASSWORD=<senha do admin do keycloak> \
  oauth
```

Normalmente, porém, este serviço é subido junto com o Keycloak pelo
`docker-compose.yml` da raiz do repositório `base` (`docker compose up`).

## Variáveis de ambiente

| Variável | Descrição | Default |
| --- | --- | --- |
| `OAUTH_INTERNAL_API_PORT` | Porta em que o Spring Boot escuta dentro do container | `8080` |
| `KEYCLOAK_SERVER_URL` | URL interna do Keycloak (nome do serviço no Docker network) | `http://localhost:8080` |
| `KEYCLOAK_REALM` | Realm usado pela aplicação | `constrsw` |
| `KEYCLOAK_CLIENT_ID` | Client confidencial cadastrado no realm | `oauth` |
| `KEYCLOAK_CLIENT_SECRET` | Secret do client `oauth` | *(obrigatório)* |
| `KEYCLOAK_ADMIN` | Usuário admin bootstrap do Keycloak (realm `master`) | `admin` |
| `KEYCLOAK_ADMIN_PASSWORD` | Senha do admin bootstrap do Keycloak | *(obrigatório)* |

## Estrutura de pacotes

```text
src/main/java/br/pucrs/constrsw/oauth/
  OauthApplication.java
  config/          # KeycloakProperties, SecurityConfig, OpenApiConfig, RestTemplate bean
  controller/       # AuthController (POST /login)
  dto/              # LoginResponse, ErrorResponse
  exception/        # OAuthApiException, GlobalExceptionHandler (@RestControllerAdvice)
  security/         # CustomAuthenticationEntryPoint (401), CustomAccessDeniedHandler (403)
  service/          # KeycloakAuthService (POST /login), KeycloakAdminService (token de admin
                     # para chamadas à Admin REST API, reaproveitável por Users/Roles)
```

## Documentação Swagger

Com o serviço no ar: `http://localhost:8181/swagger-ui.html`
(porta `OAUTH_EXTERNAL_API_PORT` conforme o `.env` da raiz).
Healthcheck do container: `http://localhost:8181/actuator/health`.

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
- Autenticação Bearer: todas as demais rotas exigem `Authorization: Bearer <token>`,
  validado como JWT do Keycloak (`spring-boot-starter-oauth2-resource-server`).
- Tratamento de erros geral: todo erro da API (validação, token ausente/inválido,
  falha ao chamar o Keycloak, exceção não tratada) responde no envelope padrão:

  ```json
  {
    "error_code": "...",
    "error_description": "...",
    "error_source": "...",
    "error_stack": ["..."]
  }
  ```

Pendente (outras pessoas do grupo, ver README da raiz do repo `base`):

- `POST/GET/PUT/PATCH/DELETE /users` (Matheus)
- `POST/GET/PUT/PATCH/DELETE /roles` + atribuição de role a usuário (Guilherme)
- Swagger completo, collection Postman, testes, tag e `.zip` de entrega (Lucas)

## Rodando só este serviço (fora do compose da raiz)

```bash
mvn spring-boot:run
# ou
docker build -t oauth .
docker run -p 8080:8080 \
  -e KEYCLOAK_URL=http://host.docker.internal:8080 \
  -e KEYCLOAK_REALM=constrsw \
  -e KEYCLOAK_CLIENT_ID=oauth \
  -e KEYCLOAK_CLIENT_SECRET=<secret> \
  oauth
```

Normalmente, porém, este serviço é subido junto com o Keycloak pelo
`docker-compose.yml` da raiz do repositório `base` (`docker compose up`).

## Variáveis de ambiente

| Variável | Descrição | Default |
| --- | --- | --- |
| `KEYCLOAK_URL` | URL interna do Keycloak (nome do serviço no Docker network) | `http://localhost:8080` |
| `KEYCLOAK_REALM` | Realm usado pela aplicação | `constrsw` |
| `KEYCLOAK_CLIENT_ID` | Client confidencial cadastrado no realm | `oauth` |
| `KEYCLOAK_CLIENT_SECRET` | Secret do client `oauth` | *(obrigatório)* |

## Estrutura de pacotes

```text
src/main/java/br/pucrs/constrsw/oauth/
  OauthApplication.java
  config/          # KeycloakProperties, SecurityConfig, OpenApiConfig, RestTemplate bean
  controller/       # AuthController (POST /login)
  dto/              # LoginResponse, ErrorResponse
  exception/        # OAuthApiException, GlobalExceptionHandler (@RestControllerAdvice)
  security/         # CustomAuthenticationEntryPoint (401), CustomAccessDeniedHandler (403)
  service/          # KeycloakAuthService (chama o token endpoint do Keycloak)
```

## Documentação Swagger

Com o serviço no ar: `http://localhost:9000/swagger-ui.html`
(porta conforme mapeada no `docker-compose.yml` da raiz).

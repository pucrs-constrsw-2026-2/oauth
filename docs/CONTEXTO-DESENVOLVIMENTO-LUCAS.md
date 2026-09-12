# Contexto Técnico e Decisões de Arquitetura — Autenticação OAuth (Grupo 08)

> **Data:** 09/09/2026  
> **Branch de Trabalho:** `grupo08-feat/lucas`  
> **Base de Comparação:** `origin/grupo08` / `main`  
> **Objetivo:** Registro detalhado de tudo o que foi implementado na feature de autenticação, o histórico de commits, a arquitetura adotada e a justificativa de por que o projeto foi separado desta forma.

---

## 1. Visão Geral da Entrega

A branch `grupo08-feat/lucas` entrega a **camada completa de autenticação e sessão do microsserviço OAuth**, atuando como um *Adapter* para a API REST do Keycloak, além de compatibilizar o container Java com a infraestrutura oficial da turma e disponibilizar uma suíte automatizada de testes funcionais.

### Escopo Atendido do Trabalho 1
Conforme especificado em [`descricao-do-trabalho1.md`](./descricao-do-trabalho1.md):
- **POST `/login`**: Autenticação de usuário via `grant_type=password` consumindo a rota de token do Keycloak.
- **POST `/refresh-token`**: Renovação de credenciais via `grant_type=refresh_token`.
- **Contrato de Erro Padrão**: Centralizado e padronizado para respostas de erro (`OA-400`, `OA-401`, `OA-502`).
- **Compatibilidade Docker Compose Central**: Execução e health check funcionais no ecossistema de microsserviços.
- **Testes Automatizados (Bruno)**: Suíte E2E automatizada validando todos os cenários felizes e de exceção.

---

## 2. Histórico de Commits e Rastreabilidade

A evolução na branch seguiu uma linha cronológica estruturada, partindo da infraestrutura até os testes de qualidade:

| Hash | Mensagem | O que foi feito |
|---|---|---|
| `ecac878` | `fix: integrar oauth ao compose compartilhado` | Criação do `docker-compose.base.override.yml` para ajustar o healthcheck para `curl`, adição de `curl` no `Dockerfile` e sincronização de portas no `application.yml`. |
| `84338df` / `18ba747` | `chore/merge: configurar gitignore do projeto` | Proteção de credenciais (`.env`), exclusão de artefatos de compilação Maven (`target/`) e arquivos temporários de sistema (`.DS_Store`). |
| `e401002` | `feat: implement Keycloak auth service and controller...` | Implementação do `AuthController`, `KeycloakAuthService`, DTOs de request/response, interceptores de erro e testes unitários com Mockito e MockMvc. |
| `9723552` | `test: add Bruno collection for OAuth flows` | Criação da coleção Bruno completa (7 requisições) com asserções, scripts de captura de variáveis e README. |
| `8a3a9ef` | `feat: add Bruno environment and login test case...` | Ajustes finos no ambiente `Local.bru` e validação do fluxo URL-encoded. |
| `8738e94` | `docs: add initial project documentation...` | Documentação de contexto inicial do grupo e requisitos da disciplina em `docs/`. |
| `5ebe567` | `feat: implement OAuth auth controller, service...` | Sincronização e consolidação das configurações de build e artefatos gerados. |

---

## 3. Por Que Foi Separado Assim? (Decisões de Arquitetura)

A arquitetura foi dividida em fronteiras claras para garantir **alta coesão, baixo acoplamento, resiliência e não-interferência** com os outros grupos da disciplina.

### 3.1 Separação do Docker Compose (`docker-compose.base.override.yml`)

- **Contexto:**  
  O repositório base compartilhado da turma fornece um `docker-compose.yml` único. Na definição original desse arquivo, o healthcheck do serviço `oauth` assumia uma aplicação Node.js:
  ```yaml
  test: ["CMD", "node", "-e", "require('http').get(...)"]
  ```
- **Por que criamos um arquivo de override separado?**  
  Modificar diretamente o `docker-compose.yml` raiz causaria conflitos de merge contínuos com as atualizações do professor e com os demais grupos.  
  Com a separação no `docker-compose.base.override.yml`:
  1. O arquivo raiz do professor permanece intacto.
  2. O override apenas substitui a propriedade `healthcheck` para utilizar `curl` chamando o endpoint `/health` exposto pelo Spring Boot.
  3. Redes, volumes, variáveis de ambiente e portas continuam 100% governadas pelo compose central.

---

### 3.2 Separação de Camadas no Backend (Clean Architecture / Ports & Adapters)

O código Java foi decomposto nos seguintes pacotes:

```
src/main/java/com/seugrupo/oauth/
├── controller/   # Entrada HTTP (Adapter de Entrada)
├── service/      # Regra de negócio e integração externa (Adapter de Saída)
├── dto/          # Contratos de dados imutáveis (Records)
├── config/       # Segurança e infraestrutura Spring
└── exception/    # Tratamento e mapeamento de falhas
```

#### A. Controller (`AuthController`)
- **Responsabilidade:** Lidar exclusivamente com o protocolo HTTP.
- **Por que aceitar dois Content-Types no `/login`?**  
  O endpoint `@PostMapping(consumes = { APPLICATION_FORM_URLENCODED_VALUE, MULTIPART_FORM_DATA_VALUE })` foi desenhado para aceitar tanto formulários codificados via URL quanto formulários multipart. Isso permite interoperabilidade com clientes legados, formulários web e ferramentas como Postman e Bruno sem exigir conversões prévias.
- **Headers e Status:** Retorna explicitamente HTTP `201 Created` no login, HTTP `200 OK` no refresh, e injeta `Cache-Control: no-store` para impedir que proxies intermediários armazenem tokens em cache.

#### B. Service (`KeycloakAuthService`)
- **Responsabilidade:** Atuar como o *Adapter* para o Keycloak.
- **Uso do `WebClient`:** Escolhido por ser moderno, eficiente e não bloqueante por padrão.
- **Por que isolar a chamada em `requestToken`?**  
  Tanto o login (`grant_type=password`) quanto o refresh (`grant_type=refresh_token`) compartilham a mesma chamada HTTP contra o endpoint OpenID Connect do Keycloak (`/realms/{realm}/protocol/openid-connect/token`), mudando apenas os parâmetros do formulário. A centralização evita duplicação de código de rede.
- **Resiliência e Timeouts:**  
  Configurado timeout estrito de 10 segundos (`TOKEN_REQUEST_TIMEOUT`). Se o Keycloak estiver fora do ar ou sobrecarregado, a chamada não trava as threads do servidor e converte a falha para HTTP `502 Bad Gateway` (`OA-502`).

#### C. DTOs Imutáveis (`records`)
- `LoginRequest`, `RefreshTokenRequest` e `TokenResponse` utilizam a funcionalidade nativa de `record` do Java 17.
- **Por que usar `records`?** Imutabilidade garantida, eliminação de boilerplate (getters, setters, hashcode) e validação declarativa com Bean Validation (`@NotBlank`).
- **Serialização snake_case:** `TokenResponse` utiliza anotações `@JsonProperty` (`access_token`, `refresh_token`, etc.) garantindo conformidade exata com o padrão RFC 6749 do OAuth2.

#### D. Segurança Local (`SecurityConfig`)
- **Por que validar JWT via JWKS local em vez de chamar `/userinfo` a cada request?**  
  O Spring Security foi configurado como um `oauth2ResourceServer` que baixa as chaves públicas do Keycloak via JWKS (`issuer-uri`). A assinatura criptográfica dos tokens é verificada em memória no próprio container.
  - *Vantagem:* Reduz drasticamente a latência e o overhead de rede sobre o Keycloak.
  - *Rotas liberadas:* `/login`, `/refresh-token`, `/health`, `/docs/**`, `/swagger-ui/**`.

#### E. Tratamento Global de Erros (`GlobalExceptionHandler` & `KeycloakErrorMapper`)
- **Por que centralizar com `@RestControllerAdvice`?**  
  Garante que nenhum erro da aplicação (seja validação, erro interno ou falha externa do Keycloak) escape sem respeitar a estrutura de erro exigida pelo contrato da disciplina:
  ```json
  {
    "error_code": "OA-401",
    "error_description": "Credenciais inválidas.",
    "error_source": "OAuthAPI.Auth",
    "error_stack": [{ "message": "..." }]
  }
  ```
- **Sanitização de Segurança:** Filtra mensagens de erro para que senhas digitadas e tokens corrompidos **nunca** apareçam refletidos no payload de erro.

---

### 3.3 Separação da Suíte de Testes Bruno (`01` a `07`)

Os testes em `backend/oauth/bruno` foram organizados numericamente de forma modular:

1. **`01 - Health`**: Teste de infraestrutura (Sanity check). Verifica se o container subiu e responde `200`.
2. **`02 - Login URL Encoded`**: Testa o fluxo feliz principal e **captura dinamicamente** o `accessToken` e o `refreshToken` na sessão de runtime do Bruno.
3. **`03 - Login Multipart`**: Valida a compatibilidade com a especificação alternativa multipart.
4. **`04 - Refresh Token`**: Consome automaticamente o `refreshToken` obtido no teste anterior, comprovando que a sessão é renovada sem intervenção manual do desenvolvedor.
5. **`05 - Login Inválido` (Negativo)**: Garante que senhas erradas resultam em `401 Unauthorized` e código `OA-401`.
6. **`06 - Refresh Sem Token` (Negativo)**: Valida a validação de schema HTTP `400 Bad Request`.
7. **`07 - Refresh Inválido` (Negativo)**: Valida que tokens rejeitados pelo Keycloak retornam erro amigável padronizado.

**Por que Bruno em vez de apenas Postman?**
- Coleção armazenada em arquivos de texto plano legíveis (`.bru`) versionados diretamente no Git.
- Possibilidade de execução headless em CI/CD via CLI (`npx @usebruno/cli`).

---

## 4. Como Executar e Validar

### 4.1 Subir o Ambiente
```bash
# 1. Garantir o volume do Keycloak
docker volume create constrsw-keycloak-data

# 2. Iniciar os serviços com o override
docker compose \
  -f docker-compose.yml \
  -f backend/oauth/docker-compose.base.override.yml \
  up -d --build keycloak oauth
```

### 4.2 Executar os Testes Automatizados
```bash
cd backend/oauth/bruno
npx --yes @usebruno/cli@4.1.0 run --env Local --bail
```
*Resultado comprovado: 7/7 testes aprovados com sucesso.*

### 4.3 Swagger UI
- Acessível no navegador em: `http://localhost:8181/docs`

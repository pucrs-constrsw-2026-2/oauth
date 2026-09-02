---
title: 'Story 1.1: Configuração do Ambiente Docker e Provisionamento do Keycloak'
type: 'feature'
created: '2026-09-02'
status: 'done'
baseline_commit: '06d2d87a3f9262a9d060de879985b189f27c581f'
review_loop_iteration: 0
context:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/architecture.md'
  - '_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O microserviço de autenticação e autorização ainda não possui um ambiente de desenvolvimento Dockerizado, e o Keycloak precisaria ser configurado manualmente por cada integrante do grupo, gerando risco de inconsistências no realm `constrsw`.

**Approach:** Criar uma infraestrutura completa multi-container com Docker Compose contendo PHP 8.2+ FPM, Nginx e Keycloak, automatizando o provisionamento de realm, client, roles, resources, policies e permissions através do arquivo `realm-export.json` carregado via `--import-realm`.

## Boundaries & Constraints

**Always:**
- A imagem do Keycloak deve ser compatível com a especificação acadêmica do professor (expondo o path `/auth/` e porta 8080).
- O realm `constrsw` deve inicializar automaticamente com o client confidencial `oauth`, roles (`administrator`, `coordinator`, `professor`, `student`), resources (`classes`, `courses`, etc.), policies e permissions.
- O ambiente deve subir de forma idempotente e reproduzível com um único comando `docker compose up -d`.
- A API deve responder na porta `http://localhost:8000`.

**Ask First:**
- Alteração da porta de exposição do Keycloak (8080) ou da API (8000).
- Modificação no nome do realm (`constrsw`) ou do client (`oauth`).

**Never:**
- Não exigir passos manuais via console web do Keycloak para configurar roles ou permissions.
- Não comitar senhas em texto puro de produção; utilizar variáveis de ambiente com `.env.example`.
- Não tocar em branches de outros grupos da turma nem na branch `main`; trabalhar estritamente na branch `grupo01`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Inicialização do Docker | Execução de `docker compose up -d` | Containers `app`, `nginx` e `keycloak` sobem em estado `healthy` | Docker relata erro claro de porta ocupada se houver conflito |
| Healthcheck da API | `GET http://localhost:8000/api/health` | HTTP 200 OK com `{ "status": "healthy", "service": "oauth" }` | Retorna 502 se o PHP-FPM estiver indisponível |
| Import do Realm Keycloak | Acesso a `http://localhost:8080/auth/realms/constrsw` | HTTP 200 OK confirmando a existência do realm e OIDC endpoints | Se falhar o import, container exibe log com erro de parsing do json |

</frozen-after-approval>

## Code Map

- `docker-compose.yml` -- Orquestração dos 3 serviços: `app` (PHP-FPM), `nginx` (web server) e `keycloak` (IdP com import).
- `docker/php/Dockerfile` -- Definição da imagem PHP 8.2-FPM com extensões necessárias (curl, mbstring, openssl) e Composer.
- `docker/nginx/default.conf` -- Configuração do servidor virtual Nginx roteando requisições para `app:9000`.
- `docker/keycloak/realm-export.json` -- Export canônico do realm `constrsw`, client `oauth`, roles, resources, policies e permissions.
- `composer.json` -- Definição de dependências PHP e autoload PSR-4 para `App\\` apontando para `src/`.
- `public/index.php` -- Front controller inicial provendo a rota de healthcheck `/api/health`.
- `src/Domain/` -- Camada de Domínio com interfaces de portas (inbound/outbound), models e exceções.
- `src/Application/DTO/` -- DTOs de comando e resposta organizados por frentes (Auth, User, Authorization).
- `src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php` -- Cliente HTTP centralizado e autenticado com Keycloak Admin API.
- `src/Infrastructure/Http/Listener/JsonExceptionListener.php` -- Listener para padronização global de erros JSON.

## Tasks & Acceptance

**Execution:**
- [x] `docker/keycloak/realm-export.json` -- Criar o JSON canônico contendo realm `constrsw`, client `oauth` com secret, roles, resources e permissions exigidos pelo professor.
- [x] `docker/php/Dockerfile` -- Criar Dockerfile para PHP 8.2-FPM com extensões PHP e Composer 2.x.
- [x] `docker/nginx/default.conf` -- Configurar Nginx para escutar na porta 80 e encaminhar requisições PHP ao container `app`.
- [x] `docker-compose.yml` -- Configurar os 3 serviços (`keycloak`, `app`, `nginx`), volumes, redes e comando `--import-realm`.
- [x] `composer.json` -- Criar configuração do Composer com autoload PSR-4 para `App\\` -> `src/`.
- [x] `src/Domain/Port/Outbound/` -- Criar interfaces `KeycloakAuthPortInterface`, `KeycloakUserPortInterface` e `KeycloakAuthorizationPortInterface`.
- [x] `src/Domain/Port/Inbound/` -- Criar interfaces dos casos de uso de Auth, User e Authorization.
- [x] `src/Domain/Model/` & `src/Domain/Exception/` -- Criar modelos de domínio (`User`, `AuthTokens`, `Role`) e exceções (`InvalidCredentialsException`, etc.).
- [x] `src/Application/DTO/` -- Criar DTOs tipados para Auth, User e Authorization.
- [x] `src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php` -- Criar cliente HTTP base para comunicação com o Keycloak.
- [x] `src/Infrastructure/Http/Listener/JsonExceptionListener.php` -- Criar listener para respostas de erro uniformes.
- [x] `public/index.php` -- Criar front controller com rota de healthcheck `/api/health`.
- [x] `.env` e `.env.example` -- Configurar variáveis de ambiente do ecossistema.

**Acceptance Criteria:**
- Given os arquivos de configuração Docker e o `realm-export.json`
- When o comando `docker compose up -d` for executado na raiz do projeto
- Then os containers `oauth-app`, `oauth-nginx` e `oauth-keycloak` iniciam com sucesso
- And `GET http://localhost:8000/api/health` retorna HTTP 200 OK
- And `GET http://localhost:8080/auth/realms/constrsw/.well-known/openid-configuration` (ou endpoint correspondente do Keycloak) responde confirmando o realm `constrsw` e o client `oauth` provisionados.

## Spec Change Log

*(Vazio - primeira iteração)*

## Design Notes

A imagem do Keycloak selecionada é configurada com `--import-realm` e a flag `KC_HTTP_RELATIVE_PATH=/auth` (se imagem Quarkus `quay.io/keycloak/keycloak:22.0.5`) para preservar a exata compatibilidade com a URL informada pelo professor (`http://localhost:8080/auth/...`). O client `oauth` tem `serviceAccountsEnabled: true` para suportar o fluxo de Client Credentials que a Admin API requer.

## Verification

**Commands:**
- `docker compose config` -- expected: Sintaxe válida do docker-compose sem erros de schema.
- `docker compose up -d` -- expected: Containers criados e em execução.
- `curl -f http://localhost:8000/api/health` -- expected: HTTP 200 com JSON status saudável.

## Suggested Review Order

**Infraestrutura e Provisionamento Keycloak**

- Orquestração multi-container para Keycloak, PHP-FPM e Nginx.
  [`docker-compose.yml:1`](../../docker-compose.yml#L1)

- Realm constrsw pré-configurado com client oauth, roles, resources e permissions.
  [`docker/keycloak/realm-export.json:1`](../../docker/keycloak/realm-export.json#L1)

- Imagem PHP 8.2-FPM e Composer 2.
  [`docker/php/Dockerfile:1`](../../docker/php/Dockerfile#L1)

- Roteamento Nginx com FastCGI para o PHP-FPM.
  [`docker/nginx/default.conf:1`](../../docker/nginx/default.conf#L1)

**Ponto de Entrada e Resiliência**

- Front controller com fallback PSR-4 e healthcheck GET /api/health.
  [`public/index.php:1`](../../public/index.php#L1)

- Interceptor global para padronização de erros JSON.
  [`src/Infrastructure/Http/Listener/JsonExceptionListener.php:1`](../../src/Infrastructure/Http/Listener/JsonExceptionListener.php#L1)

- Cliente HTTP centralizado para comunicação com a Keycloak Admin API.
  [`src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php:1`](../../src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php#L1)

**Arquitetura Hexagonal: Contratos de Portas e Modelos**

- Interfaces de saída (Outbound Ports) para Auth, User e Authorization.
  [`src/Domain/Port/Outbound/KeycloakAuthPortInterface.php:1`](../../src/Domain/Port/Outbound/KeycloakAuthPortInterface.php#L1)
  [`src/Domain/Port/Outbound/KeycloakUserPortInterface.php:1`](../../src/Domain/Port/Outbound/KeycloakUserPortInterface.php#L1)
  [`src/Domain/Port/Outbound/KeycloakAuthorizationPortInterface.php:1`](../../src/Domain/Port/Outbound/KeycloakAuthorizationPortInterface.php#L1)

- Interfaces de entrada (Inbound Ports / Use Cases) para cada operação.
  [`src/Domain/Port/Inbound/LoginUseCaseInterface.php:1`](../../src/Domain/Port/Inbound/LoginUseCaseInterface.php#L1)
  [`src/Domain/Port/Inbound/CreateUserUseCaseInterface.php:1`](../../src/Domain/Port/Inbound/CreateUserUseCaseInterface.php#L1)
  [`src/Domain/Port/Inbound/AuthorizeResourceUseCaseInterface.php:1`](../../src/Domain/Port/Inbound/AuthorizeResourceUseCaseInterface.php#L1)

- Modelos de domínio puros sem acoplamento externo.
  [`src/Domain/Model/User.php:1`](../../src/Domain/Model/User.php#L1)
  [`src/Domain/Model/AuthTokens.php:1`](../../src/Domain/Model/AuthTokens.php#L1)
  [`src/Domain/Model/Role.php:1`](../../src/Domain/Model/Role.php#L1)

---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - _bmad-output/planning-artifacts/prd.md
  - _bmad-output/planning-artifacts/architecture.md
  - docs/professor-specification.md
  - docs/team-integration-guide.md
---

# oauth - Epic Breakdown

## Overview

This document provides the complete epic and story breakdown for oauth, decomposing the requirements from the PRD and Architecture requirements into implementable stories organized in 4 decoupled streams for the 4 team members on branch `grupo01`.

## Requirements Inventory

### Functional Requirements

- **FR-1**: Autenticação de Usuário (`POST /login` recebendo `username` e `password`, consumindo endpoint OIDC do Keycloak e retornando `access_token`, `refresh_token`, `expires_in`, `token_type` "Bearer").
- **FR-2**: Renovação de Tokens (`POST /refresh` recebendo `refresh_token` e retornando novos tokens sem exigir senha).
- **FR-3**: Identificação do Usuário Logado (`GET /me` ou `/userinfo` consumindo `/protocol/openid-connect/userinfo` do Keycloak via Bearer token).
- **FR-4**: Criação de Usuário (`POST /users` via Keycloak Admin API, retornando HTTP 201 Created com os dados e ID gerado).
- **FR-5**: Listagem de Usuários Habilitados (`GET /users` retornando HTTP 200 OK com array de usuários com `enabled: true`).
- **FR-6**: Consulta de Usuário por ID (`GET /users/{id}` retornando HTTP 200 OK com os dados do usuário ou 404 Not Found).
- **FR-7**: Atualização de Atributos do Usuário (`PUT /users/{id}` atualizando nome, sobrenome ou e-mail na Admin API, retornando HTTP 200 OK ou 404 Not Found).
- **FR-8**: Atualização de Senha do Usuário (`PATCH /users/{id}` alterando credencial de senha na Admin API, retornando HTTP 200 OK ou 404 Not Found).
- **FR-9**: Exclusão Lógica de Usuário (`DELETE /users/{id}` alterando `enabled = false` no Keycloak, retornando HTTP 204 No Content ou 404 Not Found).
- **FR-10**: Validação de Acesso a Recurso (`POST /authorize` validando Bearer token e avaliando matriz de papéis `administrator`, `coordinator`, `professor`, `student` versus recursos `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`, retornando HTTP 200 OK ou 403 Forbidden).
- **FR-11**: Criação de Role (`POST /roles` via Keycloak Admin API, retornando HTTP 201 Created com os dados e ID gerado).
- **FR-12**: Listagem de Roles Ativas (`GET /roles` retornando HTTP 200 OK com array de papéis ativos).
- **FR-13**: Consulta de Role por ID (`GET /roles/{id}` retornando HTTP 200 OK com os dados do role ou 404 Not Found).
- **FR-14**: Atualização Completa de Role (`PUT /roles/{id}` atualizando atributos do papel na Admin API, retornando HTTP 200 OK ou 404 Not Found).
- **FR-15**: Atualização Parcial de Role (`PATCH /roles/{id}` atualizando campos parciais na Admin API, retornando HTTP 200 OK ou 404 Not Found).
- **FR-16**: Exclusão Lógica de Role (`DELETE /roles/{id}` inativando o papel no Keycloak sem remoção física, retornando HTTP 204 No Content ou 404 Not Found).
- **FR-17**: Atribuição de Role a Usuário (`POST /users/{userId}/roles` vinculando papel ao usuário via Keycloak Role Mapping API, retornando HTTP 200/201 ou 404 Not Found).
- **FR-18**: Revogação de Role de Usuário (`DELETE /users/{userId}/roles/{roleId}` removendo mapeamento de papel via Keycloak Role Mapping API, retornando HTTP 204 No Content ou 404 Not Found).

### NonFunctional Requirements

- **NFR-1**: Adoção estrita da Arquitetura Hexagonal (Ports & Adapters) com a camada `Domain` 100% isolada e sem acoplamento com Symfony ou Keycloak.
- **NFR-2**: Execução de todo o ambiente via Docker Compose (`docker compose up -d`) contendo PHP 8.2+ FPM, Nginx e Keycloak.
- **NFR-3**: Provisionamento automatizado do Keycloak através de `--import-realm` carregando `realm-export.json` na inicialização do container (zero setup manual).
- **NFR-4**: Autenticação administrativa com Keycloak via Service Account (`client_credentials` do client `oauth` com role `manage-users`).
- **NFR-5**: Respostas de erro padronizadas em envelope JSON (`code`, `message`, `details`) via listener global.
- **NFR-6**: Preservação histórica: exclusão de usuários e roles estritamente lógica, proibindo hard delete.

### Additional Requirements

- **AR-1**: Scaffolding Symfony 6.4 LTS / PHP 8.2+ com Nginx e PHP-FPM configurados em containers Docker.
- **AR-2**: Pré-definição de todas as Interfaces de Portas (`Domain/Port/Inbound/` e `Domain/Port/Outbound/`) e DTOs na branch base `grupo01` antes da abertura das branches dos integrantes.
- **AR-3**: Segregação de código por frentes independentes (`UseCase/Auth`, `UseCase/User`, `UseCase/Authorization`, adaptadores correspondentes e controllers isolados).
- **AR-4**: Convenção de Git Submodule da turma: branch base do time é `grupo01`; branches de features seguem `grupo01/feat/<nome>`; PRs abertos exclusivamente tendo como alvo `grupo01`.

### UX Design Requirements

- *N/A*: O projeto é uma API REST backend para microserviços. Interfaces gráficas com o usuário são explicitamente fora de escopo (Non-Goal).

### FR Coverage Map

- **FR-1**: Epic 2 - Autenticação de Usuário via `POST /login`
- **FR-2**: Epic 2 - Renovação de Tokens via `POST /refresh`
- **FR-3**: Epic 2 - Identificação do Usuário Logado via `GET /me`
- **FR-4**: Epic 3 - Criação de Usuário via `POST /users`
- **FR-5**: Epic 3 - Listagem de Usuários Habilitados via `GET /users`
- **FR-6**: Epic 3 - Consulta de Usuário por ID via `GET /users/{id}`
- **FR-7**: Epic 3 - Atualização de Atributos do Usuário via `PUT /users/{id}`
- **FR-8**: Epic 3 - Atualização de Senha do Usuário via `PATCH /users/{id}`
- **FR-9**: Epic 3 - Exclusão Lógica de Usuário via `DELETE /users/{id}`
- **FR-10**: Epic 4 - Validação de Acesso a Recursos via `POST /authorize`
- **FR-11**: Epic 6 (Story 6.1) - Criação de Role via `POST /roles`
- **FR-12**: Epic 6 (Story 6.1) - Listagem de Roles Ativas via `GET /roles`
- **FR-13**: Epic 6 (Story 6.1) - Consulta de Role por ID via `GET /roles/{id}`
- **FR-14**: Epic 6 (Story 6.2) - Atualização Completa de Role via `PUT /roles/{id}`
- **FR-15**: Epic 6 (Story 6.2) - Atualização Parcial de Role via `PATCH /roles/{id}`
- **FR-16**: Epic 6 (Story 6.2) - Exclusão Lógica de Role via `DELETE /roles/{id}`
- **FR-17**: Epic 6 (Story 6.3) - Atribuição de Role a Usuário via `POST /users/{userId}/roles`
- **FR-18**: Epic 6 (Story 6.3) - Revogação de Role de Usuário via `DELETE /users/{userId}/roles/{roleId}`
- **NFR-1 a NFR-6 / AR-1 a AR-4**: Epic 1 - Fundação, Ambiente Docker e Contratos Hexagonais

## Epic List

### Epic 1: Fundação do Microserviço, Ambiente Docker e Contratos da Arquitetura Hexagonal
Estabelecer a infraestrutura de execução conteinerizada (PHP/Symfony, Nginx e Keycloak com provisionamento automático do realm), o esqueleto de diretórios da Arquitetura Hexagonal, o tratamento global de erros e a definição formal das Interfaces de Portas e DTOs na branch base `grupo01`.
- **Atribuição:** Você (Branch base `grupo01`)
- **Requisitos Cobertos:** NFR-1 a NFR-6, AR-1 a AR-4.

### Epic 2: Autenticação de Usuários, Gestão de Tokens OIDC e Perfil
Permitir que clientes realizem login seguro via credenciais OAuth 2.0 / OpenID Connect, recebam access tokens e refresh tokens, renovem sessões ativas expiradas e consultem as informações de perfil do usuário logado.
- **Atribuição:** Integrante 2 (Branch `grupo01/feat/auth-tokens`)
- **FRs cobertos:** FR-1, FR-2, FR-3.

### Epic 3: Administração e Gestão do Ciclo de Vida de Usuários
Permitir que clientes administrativos gerenciem contas de usuários através de endpoints REST integrados à Keycloak Admin API, suportando criação, listagem de usuários ativos, busca por ID, alteração de atributos cadastrais, reset de senha e exclusão lógica segura.
- **Atribuição:** Integrante 3 (Branch `grupo01/feat/user-management`)
- **FRs cobertos:** FR-4, FR-5, FR-6, FR-7, FR-8, FR-9.

### Epic 4: Validação de Acesso a Recursos e Avaliação de Políticas
Permitir que outros microserviços do ecossistema acadêmico validem tokens de acesso e verifiquem se os papéis do usuário conferem permissão de acesso aos recursos protegidos conforme as regras institucionais, retornando 200 OK ou 403 Forbidden.
- **Atribuição:** Integrante 4 (Branch `grupo01/feat/authorization-policies`)
- **FRs cobertos:** FR-10.

### Epic 5: Observabilidade da Plataforma — Grafana
Provisionar o serviço Grafana no ambiente de desenvolvimento/demonstração (compose raiz da malha), com datasource Prometheus pré-configurado, permitindo dashboards de métricas operacionais assim que a coleta (Prometheus) estiver disponível.
- **Atribuição:** Você (Branch base `grupo01`)
- **Requisitos Cobertos:** NFR-7.

### Epic 6: Gestão do Ciclo de Vida de Roles e Atribuição de Papéis a Usuários
Permitir a administração centralizada de papéis (criação, listagem, busca por ID, atualização integral/parcial e exclusão lógica) e a gestão dinâmica das atribuições de roles a usuários no Keycloak.
- **Atribuição:** Desenvolvedor Backend (Branch `grupo01/feat/roles-management`)
- **FRs cobertos:** FR-11, FR-12, FR-13, FR-14, FR-15, FR-16, FR-17, FR-18.

---

## Epic 1: Fundação do Microserviço, Ambiente Docker e Contratos da Arquitetura Hexagonal

**Objetivo do Épico:** Estabelecer a infraestrutura conteinerizada e os contratos fundamentais da Arquitetura Hexagonal na branch base `grupo01`, permitindo que os outros 3 integrantes iniciem seus desenvolvimentos em paralelo com contratos consistentes e sem conflitos de merge.

### Story 1.1: Configuração do Ambiente Docker e Provisionamento do Keycloak

As a desenvolvedor do time,
I want um ambiente multi-container gerenciado pelo Docker Compose contendo PHP 8.2+ Symfony, Nginx e Keycloak pré-configurado,
So that eu possa iniciar todo o ecossistema com um único comando sem precisar configurar nada manualmente na interface do Keycloak.

**Acceptance Criteria:**

**Given** os arquivos `docker-compose.yml`, `docker/php/Dockerfile`, `docker/nginx/default.conf` e `docker/keycloak/realm-export.json`
**When** o desenvolvedor executa `docker compose up -d` na raiz do projeto
**Then** os serviços `app` (PHP-FPM), `nginx` (porta 8000) e `keycloak` (porta 8080 com suporte ao prefixo `/auth`) inicializam em estado saudável
**And** o Keycloak carrega automaticamente o realm `constrsw`, o client confidencial `oauth` (com secret gerado), os roles (`administrator`, `coordinator`, `professor`, `student`), os resources com suas URLs, as policies de role e as permissions associadas
**And** o endpoint de health check da API em `http://localhost:8000/api/health` (ou rota base `/`) responde com status HTTP 200.

### Story 1.2: Estrutura Base da Arquitetura Hexagonal, Interfaces de Portas e DTOs

As a desenvolvedor do time,
I want o esqueleto de diretórios da Arquitetura Hexagonal criado com todas as Interfaces de Portas (*Inbound* e *Outbound*) e DTOs compartilhados,
So that os 4 integrantes do time possam implementar seus casos de uso e adaptadores com contratos firmes e sem sobreposição de arquivos.

**Acceptance Criteria:**

**Given** a branch base `grupo01`
**When** a estrutura de namespaces e diretórios for verificada em `src/`
**Then** as interfaces de saída (*Outbound Ports*) `KeycloakAuthPortInterface`, `KeycloakUserPortInterface` e `KeycloakAuthorizationPortInterface` estarão criadas em `src/Domain/Port/Outbound/`
**And** as interfaces de entrada (*Inbound Ports*) para cada caso de uso estarão criadas em `src/Domain/Port/Inbound/`
**And** os DTOs compartilhados de entrada e saída estarão declarados em `src/Application/DTO/`
**And** os modelos de domínio puros (`User`, `AuthTokens`, `Role`) estarão declarados em `src/Domain/Model/` sem nenhuma dependência de bibliotecas externas ou do Symfony.

### Story 1.3: Cliente HTTP Base do Keycloak e Interceptor Global de Erros JSON

As a desenvolvedor do time,
I want uma classe base `KeycloakHttpClient` autenticada via *Client Credentials* e um `JsonExceptionListener` global,
So that as chamadas administrativas ao Keycloak ocorram com token de serviço transparente e todos os erros da API retornem no envelope JSON padronizado.

**Acceptance Criteria:**

**Given** as credenciais do client confidencial `oauth` configuradas nas variáveis de ambiente da aplicação
**When** o serviço `KeycloakHttpClient` invoca rotas da Admin API do Keycloak (`/admin/realms/constrsw/...`)
**Then** ele obtém e injeta automaticamente o token OAuth2 obtido via `grant_type: client_credentials`, renovando-o quando expirado
**And** se qualquer controller ou use case lançar uma exceção não tratada ou exceção de domínio, o `JsonExceptionListener` captura o erro e retorna a resposta com o código HTTP adequado no formato:
```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Mensagem descritiva",
    "details": []
  }
}
```

---

## Epic 2: Autenticação de Usuários, Gestão de Tokens OIDC e Perfil

**Objetivo do Épico:** Implementar os endpoints de autenticação via credenciais, renovação de tokens expirados e obtenção de perfil do usuário logado na branch `grupo01/feat/auth-tokens`.

### Story 2.1: Autenticação de Usuário via Login (POST /login)

As a cliente da API,
I want enviar credenciais (`username` e `password`) para o endpoint `POST /login`,
So that eu receba Access Token e Refresh Token válidos emitidos pelo Keycloak.

**Acceptance Criteria:**

**Given** um usuário cadastrado e ativo no realm `constrsw`
**When** uma requisição `POST /login` for enviada com `username` e `password` válidos (via JSON ou form-data)
**Then** o `AuthController` aciona o `LoginUseCase` que invoca o `KeycloakAuthAdapter`
**And** a API retorna status HTTP 200 (OK) com corpo JSON contendo:
```json
{
  "token_type": "Bearer",
  "access_token": "...",
  "expires_in": 300,
  "refresh_token": "...",
  "refresh_expires_in": 1800
}
```
**And** se as credenciais forem incorretas ou o usuário estiver desabilitado, retorna status HTTP 401 (Unauthorized)
**And** se os parâmetros obrigatórios forem omitidos, retorna status HTTP 400 (Bad Request).

### Story 2.2: Renovação de Sessão via Refresh Token (POST /refresh)

As a cliente da API com um Access Token expirado,
I want enviar meu `refresh_token` para o endpoint `POST /refresh`,
So that eu receba novos tokens sem precisar submeter novamente meu usuário e senha.

**Acceptance Criteria:**

**Given** uma sessão com `refresh_token` válido
**When** uma requisição `POST /refresh` for enviada com o campo `refresh_token`
**Then** o `RefreshTokenUseCase` aciona o Keycloak via `grant_type: refresh_token` e retorna HTTP 200 (OK) com o novo par de tokens
**And** se o `refresh_token` estiver expirado, revogado ou inválido, retorna HTTP 401 (Unauthorized)
**And** se o payload não contiver o `refresh_token`, retorna HTTP 400 (Bad Request).

### Story 2.3: Consulta de Perfil do Usuário Autenticado (GET /me)

As a cliente autenticado,
I want enviar meu Access Token para o endpoint `GET /me`,
So that eu obtenha as informações de identidade do usuário logado.

**Acceptance Criteria:**

**Given** um header `Authorization: Bearer <valid_access_token>`
**When** a requisição `GET /me` (ou `GET /userinfo`) for recebida
**Then** o `GetUserInfoUseCase` consome o endpoint `/protocol/openid-connect/userinfo` do Keycloak e responde HTTP 200 (OK) com os dados do usuário (`sub`, `name`, `email`, `preferred_username`)
**And** se o token for omitido, estiver expirado ou possuir assinatura inválida, retorna HTTP 401 (Unauthorized).

---

## Epic 3: Administração e Gestão do Ciclo de Vida de Usuários

**Objetivo do Épico:** Implementar as operações de CRUD e ciclo de vida de contas de usuários integradas à Keycloak Admin REST API na branch `grupo01/feat/user-management`.

### Story 3.1: Cadastro de Novos Usuários (POST /users)

As a administrador da aplicação,
I want enviar os dados de um novo usuário para `POST /users`,
So that a conta seja provisionada na base do Keycloak com credenciais ativas.

**Acceptance Criteria:**

**Given** um header com Bearer Token válido
**When** uma requisição `POST /users` for enviada com JSON contendo `username` (e-mail), `firstName`, `lastName`, `email` e `password`
**Then** o `CreateUserUseCase` utiliza o `KeycloakUserAdapter` para cadastrar o usuário via Admin API do Keycloak
**And** a API responde com status HTTP 201 (Created) e o JSON do usuário contendo o `id` gerado
**And** se o e-mail já existir no realm, a API responde HTTP 409 (Conflict)
**And** se campos obrigatórios forem inválidos, responde HTTP 400 (Bad Request).

### Story 3.2: Listagem de Usuários Ativos e Busca por ID (GET /users e GET /users/{id})

As a administrador da aplicação,
I want listar os usuários ativos e consultar os detalhes de um usuário pelo seu ID,
So that eu possa visualizar o diretório de contas institucionais.

**Acceptance Criteria:**

**Given** uma requisição com Bearer Token válido
**When** a rota `GET /users` for acionada
**Then** o `ListUsersUseCase` consulta a Admin API do Keycloak e retorna HTTP 200 (OK) com a lista contendo exclusivamente usuários habilitados (`enabled: true`)
**And** when a rota `GET /users/{id}` for acionada com um ID existente
**Then** o `GetUserByIdUseCase` retorna HTTP 200 (OK) com o JSON do usuário
**And** se o ID não existir no Keycloak, retorna HTTP 404 (Not Found).

### Story 3.3: Atualização de Atributos e Senha de Usuário (PUT /users/{id} e PATCH /users/{id})

As a administrador da aplicação,
I want atualizar atributos cadastrais e resetar a senha de um usuário existente,
So that eu possa manter o cadastro do usuário atualizado e redefinir suas credenciais quando necessário.

**Acceptance Criteria:**

**Given** um Bearer Token válido e um usuário com identificador `{id}` existente
**When** uma requisição `PUT /users/{id}` for enviada com novos dados (nome, sobrenome, e-mail)
**Then** o `UpdateUserUseCase` aciona o método PUT da Admin API do Keycloak e responde HTTP 200 (OK) com corpo vazio
**And** when uma requisição `PATCH /users/{id}` for enviada com o payload `{"password": "novaSenhaSegura123"}`
**Then** o `UpdatePasswordUseCase` aciona o endpoint de redefinição de credencial na Admin API do Keycloak e responde HTTP 200 (OK) com corpo vazio
**And** se o `{id}` for inexistente em qualquer das requisições, responde HTTP 404 (Not Found)
**And** se a senha for vazia ou inválida, responde HTTP 400 (Bad Request).

### Story 3.4: Exclusão Lógica de Usuário (DELETE /users/{id})

As a administrador da aplicação,
I want desativar um usuário através do endpoint `DELETE /users/{id}`,
So that o usuário perca o acesso ao sistema sem que seus registros históricos sejam excluídos fisicamente do Identity Provider.

**Acceptance Criteria:**

**Given** um usuário cadastrado com status ativo
**When** a requisição `DELETE /users/{id}` for executada
**Then** o `DisableUserUseCase` consome a Admin API do Keycloak alterando o atributo do usuário para `enabled: false`
**And** a API retorna status HTTP 204 (No Content) com corpo vazio
**And** o usuário desabilitado não é retornado na listagem `GET /users` e é rejeitado em tentativas de `POST /login`
**And** se o `{id}` não for encontrado no Keycloak, responde HTTP 404 (Not Found).

---

## Epic 4: Validação de Acesso a Recursos e Avaliação de Políticas

**Objetivo do Épico:** Implementar o endpoint verificador de permissões e políticas de acesso na branch `grupo01/feat/authorization-policies`.

### Story 4.1: Validação de Acesso e Avaliação de Políticas por Recurso (POST /authorize)

As a microserviço da aplicação acadêmica,
I want verificar se o Access Token do usuário permite acessar determinado recurso enviando uma requisição para `POST /authorize`,
So that meu serviço possa autorizar ou bloquear a operação de negócio conforme os papéis institucionais do usuário.

**Acceptance Criteria:**

**Given** uma requisição contendo o header `Authorization: Bearer <token>` e o corpo JSON:
```json
{
  "resource": "rooms"
}
```
**When** o `AuthorizeResourceUseCase` valida o token e avalia os papéis do usuário contra a matriz de permissões:
* Papel `administrator`: recursos `resources`, `rooms`, `professors`, `students`
* Papel `coordinator`: recursos `courses`, `classes`
* Papel `professor`: recursos `lessons`, `reservations`
**Then** se o usuário possuir papel com permissão sobre o recurso solicitado, a API responde status HTTP 200 (OK)
**And** se o usuário autenticado NÃO possuir permissão para o recurso solicitado (ex: role `student` solicitando `rooms`, ou role `professor` solicitando `classes`), a API responde status HTTP 403 (Forbidden)
**And** se o token for omitido, estiver expirado ou possuir assinatura inválida, a API responde HTTP 401 (Unauthorized)
**And** se o campo `resource` for omitido ou inválido, a API responde HTTP 400 (Bad Request).

---

## Epic 5: Observabilidade da Plataforma — Grafana

**Objetivo do Épico:** Disponibilizar visualização de métricas operacionais via Grafana no ambiente compartilhado da malha, sem bloquear na existência do coletor Prometheus. A instrumentação do oauth (endpoint `/metrics`) e o serviço Prometheus são responsabilidade da frente de trabalho do Prometheus, não desta story.

### Story 5.1: Provisionamento do Serviço Grafana no Compose Raiz

Como desenvolvedor do time,
Eu quero um serviço Grafana pré-configurado no `docker-compose.yml` raiz, com datasource Prometheus e provisioning automático,
Para que dashboards de métricas fiquem disponíveis assim que a coleta de métricas existir, sem setup manual.

**Acceptance Criteria:**

**Given** o `docker-compose.yml` raiz da malha
**When** o desenvolvedor executa `docker compose up -d grafana`
**Then** o serviço `grafana` inicializa em estado saudável, acessível na porta dedicada
**And** o datasource Prometheus já está provisionado (apontando para o host `prometheus`), mesmo que o serviço `prometheus` ainda não exista
**And** nenhuma configuração manual é necessária na primeira execução (usuário/senha via variáveis de ambiente).

---

### Story 5.2: Instrumentação de Métricas do OAuth e Exposição no Padrão Prometheus

Como desenvolvedor ou operador da plataforma,
Eu quero que o microserviço `oauth` exponha métricas de runtime PHP, tráfego HTTP e operações do serviço no endpoint `GET /metrics`,
Para que o serviço coletor Prometheus possa raspar os dados operacionais e alimentar dashboards no Grafana.

**Acceptance Criteria:**

**Given** o microserviço `oauth` em execução
**When** uma requisição `GET /metrics` for enviada
**Then** a API responde HTTP 200 (OK) com header `Content-Type: text/plain; version=0.0.4; charset=utf-8`
**And** o corpo da resposta contém as famílias de métricas do runtime PHP (`php_info`, `php_memory_bytes`, `php_memory_peak_bytes`)
**And** o corpo contém os metadados do serviço (`oauth_service_info`) com labels de serviço, framework e realm
**And** requisições HTTP anteriores são contabilizadas em `http_requests_total` com labels `method`, `route` e `status`
**And** a duração das requisições é registrada em `http_request_duration_seconds`
**And** requisições ao próprio `/metrics` não inflam as métricas de tráfego de negócio.

---

## Epic 6: Gestão do Ciclo de Vida de Roles e Atribuição de Papéis a Usuários

**Objetivo do Épico:** Implementar a gestão completa de papéis (roles) e suas associações a usuários no Keycloak através de endpoints REST padronizados, seguindo estritamente a Arquitetura Hexagonal, dividida em 3 frentes/histórias autônomas e testáveis.

### Story 6.1: Criação e Consulta de Roles (Parte 1)

Como administrador da plataforma,
Eu quero cadastrar novos papéis no Keycloak e consultar roles cadastradas por ID ou em listagem geral,
Para que o sistema possua os papéis necessários para parametrização de permissões e controle de acesso.

**Acceptance Criteria:**

**Given** um administrador autenticado com Bearer Token válido
**When** envia uma requisição `POST /roles` com JSON válido contendo `name` e opcionalmente `description`
**Then** a API cadastra o role no Keycloak e responde HTTP 201 (Created) com os dados do role incluindo o `id` gerado
**And** se já existir um role com o mesmo nome, a API responde HTTP 409 (Conflict) com erro padronizado.

**Given** um administrador autenticado
**When** envia uma requisição `GET /roles`
**Then** a API responde HTTP 200 (OK) com um array JSON contendo todas as roles ativas cadastradas no realm
**And** roles que foram inativadas/excluídas logicamente não devem ser exibidas na listagem.

**Given** um administrador autenticado
**When** envia uma requisição `GET /roles/{id}` com um ID existente
**Then** a API responde HTTP 200 (OK) com a representação detalhada do role correspondente
**And** se o ID informado não existir no Keycloak, a API responde HTTP 404 (Not Found).

**Given** uma requisição para qualquer um dos endpoints acima
**When** o token for omitido ou inválido
**Then** a API responde HTTP 401 (Unauthorized).

---

### Story 6.2: Atualização e Exclusão Lógica de Roles (Parte 2)

Como administrador da plataforma,
Eu quero atualizar atributos de um papel (de forma integral ou parcial) e realizar sua exclusão lógica,
Para manter o catálogo de papéis atualizado sem perder histórico ou integridade relacional.

**Acceptance Criteria:**

**Given** um administrador autenticado
**When** envia uma requisição `PUT /roles/{id}` com payload JSON válido contendo `name` e `description`
**Then** a API atualiza integralmente o role na Admin API do Keycloak e responde HTTP 200 (OK) com os dados atualizados
**And** se o ID do role não existir, a API responde HTTP 404 (Not Found).

**Given** um administrador autenticado
**When** envia uma requisição `PATCH /roles/{id}` com payload parcial (ex: apenas `description` ou apenas `name`)
**Then** a API atualiza somente os campos informados mantendo os demais inalterados e responde HTTP 200 (OK)
**And** se o ID do role não existir, a API responde HTTP 404 (Not Found).

**Given** um administrador autenticado
**When** envia uma requisição `DELETE /roles/{id}` para um role existente
**Then** a API realiza a inativação lógica do role no Keycloak (sem hard-delete) e responde HTTP 204 (No Content) com corpo vazio
**And** requisições posteriores a `GET /roles` deixam de listar o role inativado
**And** se o ID do role não existir, a API responde HTTP 404 (Not Found).

---

### Story 6.3: Atribuição e Desassociação de Roles a Usuários (Parte 3)

Como administrador da plataforma,
Eu quero atribuir papéis a usuários e revogar atribuições existentes,
Para que as políticas de autorização reflitam dinamicamente as funções desempenhadas por cada usuário.

**Acceptance Criteria:**

**Given** um administrador autenticado
**When** envia uma requisição `POST /users/{userId}/roles` com payload JSON contendo o identificador do papel (`roleId` ou `name`)
**Then** a API associa o papel ao usuário através da Role Mapping API do Keycloak e responde HTTP 200 (OK) ou HTTP 201 (Created)
**And** se o `userId` não existir no Keycloak, a API responde HTTP 404 (Not Found)
**And** se o role informado não existir no Keycloak, a API responde HTTP 404 (Not Found).

**Given** um administrador autenticado
**When** envia uma requisição `DELETE /users/{userId}/roles/{roleId}`
**Then** a API revoga a atribuição do papel do usuário no Keycloak e responde HTTP 204 (No Content) com corpo vazio
**And** se o usuário ou o role não forem localizados, a API responde HTTP 404 (Not Found).

**Given** uma requisição para os endpoints de mapeamento role-user
**When** o header `Authorization` for ausente ou portar token inválido/expirado
**Then** a API responde HTTP 401 (Unauthorized).



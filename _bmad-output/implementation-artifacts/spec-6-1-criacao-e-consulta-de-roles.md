---
title: 'Story 6.1: Criação e Consulta de Roles'
type: 'feature'
created: '2026-09-16'
status: 'done'
baseline_commit: '972cb39d4906d8bc11bd7755ae6b8bfef784f42a'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/epic-6-context.md'
  - '_bmad-output/planning-artifacts/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O microserviço `oauth` atualmente não oferece endpoints REST para gerenciar Realm Roles no Keycloak, impedindo que administradores criem e consultem papéis institucionais de forma padronizada via API.

**Approach:** Implementar os contratos e endpoints de criação e consulta de roles (`POST /roles`, `GET /roles`, `GET /roles/{id}`) seguindo a Arquitetura Hexagonal, integrando com a Keycloak Admin REST API (`/admin/realms/{realm}/roles` e `/admin/realms/{realm}/roles-by-id/{id}`).

## Boundaries & Constraints

**Always:**
- Adoção estrita da Arquitetura Hexagonal: camada `Domain` 100% isolada, sem imports de Symfony ou Keycloak.
- Todas as rotas de roles exigem autenticação via Bearer Token válido no header `Authorization`. Tokens ausentes ou inválidos respondem HTTP 401 Unauthorized.
- Respostas de erro padronizadas pelo `JsonExceptionListener` no formato `error.code`, `error.message` e `error.details`.
- Injeção e amarração de interfaces no Symfony container via `config/services.yaml`.

**Ask First:**
- Nenhuma alteração nos contratos dos Épicos 1 a 5 ou no realm export original.

**Never:**
- Não executar hard delete de papéis.
- Não acoplar controllers ou casos de uso diretamente ao `KeycloakHttpClient` ou classes de infraestrutura.
- Não retornar stack traces ou respostas HTML.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Criação de role com sucesso | `POST /roles` com `{"name": "editor", "description": "Editor de conteúdo"}` | HTTP 201 Created com JSON `{"id": "...", "name": "editor", "description": "...", "enabled": true}` | N/A |
| Criação com nome duplicado | `POST /roles` com nome já existente no Keycloak | HTTP 409 Conflict | `ROLE_ALREADY_EXISTS` |
| Criação com nome vazio ou ausente | `POST /roles` com `{"name": ""}` | HTTP 400 Bad Request | `VALIDATION_ERROR` com detalhe no campo `name` |
| Listagem de roles ativas | `GET /roles` com Bearer token | HTTP 200 OK com array JSON de `RoleDTO` | N/A |
| Busca de role por ID existente | `GET /roles/{id}` com UUID válido | HTTP 200 OK com JSON de `RoleDTO` | N/A |
| Busca de role por ID inexistente | `GET /roles/{id}` com ID inexistente | HTTP 404 Not Found | `ROLE_NOT_FOUND` |
| Requisição sem Bearer Token | Qualquer endpoint de roles sem header `Authorization` | HTTP 401 Unauthorized | `INVALID_TOKEN` |

</frozen-after-approval>

## Code Map

- `src/Domain/Model/Role.php` -- Enum/Modelo existente; preservar casos nativos e garantir suporte a entidades dinâmicas de role.
- `src/Domain/Exception/RoleNotFoundException.php` -- Exceção de domínio HTTP 404 com código `ROLE_NOT_FOUND`.
- `src/Domain/Exception/RoleAlreadyExistsException.php` -- Exceção de domínio HTTP 409 com código `ROLE_ALREADY_EXISTS`.
- `src/Domain/Port/Inbound/CreateRoleUseCaseInterface.php` -- Contrato do caso de uso de criação de role.
- `src/Domain/Port/Inbound/ListRolesUseCaseInterface.php` -- Contrato do caso de uso de listagem de roles.
- `src/Domain/Port/Inbound/GetRoleByIdUseCaseInterface.php` -- Contrato do caso de uso de busca por ID.
- `src/Domain/Port/Outbound/KeycloakRolePortInterface.php` -- Contrato da porta outbound para operações no Keycloak.
- `src/Application/DTO/Role/RoleDTO.php` -- DTO de representação de Role (`id`, `name`, `description`, `enabled`).
- `src/Application/DTO/Role/CreateRoleDTO.php` -- DTO de entrada para criação de role (`name`, `description`).
- `src/Application/UseCase/Role/CreateRoleUseCase.php` -- Validação de entrada e orquestração de criação.
- `src/Application/UseCase/Role/ListRolesUseCase.php` -- Orquestração da listagem de roles ativas.
- `src/Application/UseCase/Role/GetRoleByIdUseCase.php` -- Busca por ID e lançamento de `RoleNotFoundException`.
- `src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php` -- Ajuste não-destrutivo em `requestAdmin` para suportar tratamento de erros customizados de roles.
- `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php` -- Adaptador que consome as APIs de Realm Roles do Keycloak.
- `src/Infrastructure/Http/Controller/RoleController.php` -- Controlador REST Symfony para `/roles`.
- `config/services.yaml` -- Registro e amarração de DI para as portas inbound/outbound de Role.

## Tasks & Acceptance

**Execution:**
- [x] `src/Domain/Exception/RoleNotFoundException.php` -- Criar exceção de domínio HTTP 404 com código `ROLE_NOT_FOUND`.
- [x] `src/Domain/Exception/RoleAlreadyExistsException.php` -- Criar exceção de domínio HTTP 409 com código `ROLE_ALREADY_EXISTS`.
- [x] `src/Application/DTO/Role/RoleDTO.php` -- Criar DTO com `id`, `name`, `description`, `enabled` e método `toArray()`.
- [x] `src/Application/DTO/Role/CreateRoleDTO.php` -- Criar DTO de entrada com `name`, `description` e factory `fromArray()`.
- [x] `src/Domain/Port/Inbound/CreateRoleUseCaseInterface.php` -- Declarar interface para criação de role.
- [x] `src/Domain/Port/Inbound/ListRolesUseCaseInterface.php` -- Declarar interface para listagem de roles.
- [x] `src/Domain/Port/Inbound/GetRoleByIdUseCaseInterface.php` -- Declarar interface para busca de role por ID.
- [x] `src/Domain/Port/Outbound/KeycloakRolePortInterface.php` -- Declarar porta outbound com `createRole`, `listActiveRoles` e `getRoleById`.
- [x] `src/Application/UseCase/Role/CreateRoleUseCase.php` -- Implementar caso de uso com validação de campo obrigatório (`name`).
- [x] `src/Application/UseCase/Role/ListRolesUseCase.php` -- Implementar caso de uso de listagem de roles.
- [x] `src/Application/UseCase/Role/GetRoleByIdUseCase.php` -- Implementar caso de uso de busca por ID com verificação de não-nulo.
- [x] `src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php` -- Adicionar parâmetro opcional `$mapUserExceptions = true` para permitir tratamento fino em adaptadores de roles.
- [x] `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php` -- Implementar adaptador conectando a `admin/realms/{realm}/roles` e `admin/realms/{realm}/roles-by-id/{id}`.
- [x] `src/Infrastructure/Http/Controller/RoleController.php` -- Implementar endpoints `POST /roles`, `GET /roles` e `GET /roles/{id}`.
- [x] `config/services.yaml` -- Mapear interfaces inbound e outbound de Role no container Symfony.
- [x] `tests/Unit/Application/UseCase/Role/CreateRoleUseCaseTest.php` -- Testes unitários para criação de role e validações.
- [x] `tests/Unit/Application/UseCase/Role/GetRoleByIdUseCaseTest.php` -- Testes unitários para busca por ID e erro 404.
- [x] `tests/Unit/Application/UseCase/Role/ListRolesUseCaseTest.php` -- Testes unitários para listagem de roles.
- [x] `tests/Unit/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapterTest.php` -- Testes unitários do adaptador Keycloak com mocks.
- [x] `tests/Integration/Infrastructure/Http/Controller/RoleControllerTest.php` -- Testes de integração WebTestCase para os 3 endpoints.

**Acceptance Criteria:**
- Given um payload válido com `name`, when `POST /roles`, then responde HTTP 201 Created com os dados da role e seu `id`.
- Given um payload com nome já existente, when `POST /roles`, then responde HTTP 409 Conflict com código `ROLE_ALREADY_EXISTS`.
- Given um payload sem campo `name`, when `POST /roles`, then responde HTTP 400 Bad Request com código `VALIDATION_ERROR`.
- Given requisição `GET /roles`, when executada, then responde HTTP 200 OK com array contendo todas as roles ativas.
- Given um ID existente de role, when `GET /roles/{id}`, then responde HTTP 200 OK com os detalhes da role.
- Given um ID inexistente de role, when `GET /roles/{id}`, then responde HTTP 404 Not Found com código `ROLE_NOT_FOUND`.

## Design Notes

- Keycloak `POST /admin/realms/{realm}/roles` responde 201 Created com header `Location` no formato `.../roles/{role-name}`. Para obter o ID gerado, o adaptador inspeciona o header `Location` ou efetua lookup em `GET /admin/realms/{realm}/roles/{role-name}`.
- Em `KeycloakHttpClient`, o método `requestAdmin` recebe `$mapUserExceptions = true` por padrão; `KeycloakRoleAdapter` passa `false` para capturar 404 e 409 e convertê-los especificamente em `RoleNotFoundException` e `RoleAlreadyExistsException`.

## Verification

**Commands:**
- `./bin/phpunit` -- expected: Todos os testes existentes e novos testes de Role executam com 100% de sucesso.

## Suggested Review Order

**Contratos e Domínio**

- Interface outbound para operações de Realm Roles no Keycloak
  [`KeycloakRolePortInterface.php:11`](../../src/Domain/Port/Outbound/KeycloakRolePortInterface.php#L11)
- Exceção de domínio para papéis não encontrados (HTTP 404)
  [`RoleNotFoundException.php:7`](../../src/Domain/Exception/RoleNotFoundException.php#L7)
- Exceção de domínio para papéis duplicados (HTTP 409)
  [`RoleAlreadyExistsException.php:7`](../../src/Domain/Exception/RoleAlreadyExistsException.php#L7)
- Interfaces inbound dos casos de uso de criação, listagem e busca
  [`CreateRoleUseCaseInterface.php:10`](../../src/Domain/Port/Inbound/CreateRoleUseCaseInterface.php#L10)
  [`ListRolesUseCaseInterface.php:10`](../../src/Domain/Port/Inbound/ListRolesUseCaseInterface.php#L10)
  [`GetRoleByIdUseCaseInterface.php:10`](../../src/Domain/Port/Inbound/GetRoleByIdUseCaseInterface.php#L10)

**Aplicação e Casos de Uso**

- DTOs de entrada e representação de papéis
  [`RoleDTO.php:7`](../../src/Application/DTO/Role/RoleDTO.php#L7)
  [`CreateRoleDTO.php:7`](../../src/Application/DTO/Role/CreateRoleDTO.php#L7)
- Caso de uso de criação com validação de campo obrigatório
  [`CreateRoleUseCase.php:13`](../../src/Application/UseCase/Role/CreateRoleUseCase.php#L13)
- Casos de uso de listagem e busca por ID com verificação de não-nulo
  [`ListRolesUseCase.php:11`](../../src/Application/UseCase/Role/ListRolesUseCase.php#L11)
  [`GetRoleByIdUseCase.php:12`](../../src/Application/UseCase/Role/GetRoleByIdUseCase.php#L12)

**Infraestrutura e Keycloak**

- Controlador REST expondo rotas POST /roles, GET /roles e GET /roles/{id}
  [`RoleController.php:18`](../../src/Infrastructure/Http/Controller/RoleController.php#L18)
- Adaptador Keycloak com mapeamento para Admin API e filtro de soft-delete
  [`KeycloakRoleAdapter.php:13`](../../src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php#L13)
- Ajuste não-destrutivo em requestAdmin para suportar exceções customizadas
  [`KeycloakHttpClient.php:132`](../../src/Infrastructure/Keycloak/Client/KeycloakHttpClient.php#L132)
- Mapeamento de injeção de dependência das novas portas no Symfony
  [`services.yaml:65`](../../config/services.yaml#L65)

**Testes Automatizados**

- Testes unitários dos casos de uso e adaptador
  [`CreateRoleUseCaseTest.php:15`](../../tests/Unit/Application/UseCase/Role/CreateRoleUseCaseTest.php#L15)
  [`KeycloakRoleAdapterTest.php:14`](../../tests/Unit/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapterTest.php#L14)
- Testes de integração cobrindo ciclo de vida completo e códigos HTTP
  [`RoleControllerTest.php:11`](../../tests/Integration/Infrastructure/Http/Controller/RoleControllerTest.php#L11)


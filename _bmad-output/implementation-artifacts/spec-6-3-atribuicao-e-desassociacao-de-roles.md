---
title: 'Story 6.3: Atribuição e Desassociação de Roles a Usuários'
type: 'feature'
created: '2026-09-16'
status: 'done'
baseline_commit: 'b9ec28d'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/epic-6-context.md'
  - '_bmad-output/planning-artifacts/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Administradores não dispõem de endpoints na API do microserviço `oauth` para atribuir papéis a usuários ou revogar papéis já concedidos, impossibilitando a gestão de permissões e controle de acesso dinâmico diretamente pela API.

**Approach:** Implementar os endpoints `POST /users/{userId}/roles` (atribuição de role) e `DELETE /users/{userId}/roles/{roleId}` (desassociação de role) integrando com o endpoint de Role Mappings do Keycloak Admin REST API (`/admin/realms/{realm}/users/{userId}/role-mappings/realm`), garantindo validação de existência de usuário e role, compatibilidade com busca por UUID e por nome, e documentação OpenAPI no Swagger.

## Boundaries & Constraints

**Always:**
- Adoção estrita da Arquitetura Hexagonal: regras de negócio e validações na camada de aplicação/domínio desacopladas do framework e do Keycloak.
- Se o `userId` não existir no Keycloak, retornar HTTP 404 `USER_NOT_FOUND`.
- Se o role (`roleId` ou `name`) não existir ou estiver logicamente inativado, retornar HTTP 404 `ROLE_NOT_FOUND`.
- `POST /users/{userId}/roles` responde HTTP 200 (ou 201) com JSON confirmando o vínculo.
- `DELETE /users/{userId}/roles/{roleId}` revoga a atribuição no Keycloak e responde HTTP 204 No Content com corpo vazio.
- Todas as operações exigem Bearer token válido no header `Authorization`. Tokens ausentes ou inválidos respondem HTTP 401 Unauthorized.
- Documentação completa das rotas e schemas no Swagger UI (`/docs`) e na rota OpenAPI (`/docs/openapi.json`).

**Ask First:**
- Nenhuma decisão divergente identificada para esta estória.

**Never:**
- Nunca executar hard delete ou alteração direta de banco de dados.
- Nunca acoplar detalhes de infraestrutura HTTP nas interfaces de domínio.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Atribuição com roleId (UUID) | `POST /users/{userId}/roles` com `{"roleId": "uuid"}` | Role associado ao usuário no Keycloak | HTTP 200 com JSON de confirmação |
| Atribuição com nome do role | `POST /users/{userId}/roles` com `{"name": "professor"}` | Role localizado pelo nome e associado ao usuário | HTTP 200 com JSON de confirmação |
| Atribuição sem identificador do role | `POST /users/{userId}/roles` com `{}` ou campo vazio | Rejeição por validação de entrada | HTTP 400 `VALIDATION_ERROR` |
| Atribuição para usuário inexistente | `POST /users/inexistente/roles` com role válido | Falha por usuário não encontrado | HTTP 404 `USER_NOT_FOUND` |
| Atribuição com role inexistente ou desativado | `POST /users/{userId}/roles` com role inexistente | Falha por role não encontrado | HTTP 404 `ROLE_NOT_FOUND` |
| Desassociação com sucesso | `DELETE /users/{userId}/roles/{roleId}` para vínculo existente | Role revogado do usuário no Keycloak | HTTP 204 No Content com corpo vazio |
| Desassociação com usuário inexistente | `DELETE /users/inexistente/roles/{roleId}` | Falha por usuário não encontrado | HTTP 404 `USER_NOT_FOUND` |
| Desassociação com role inexistente | `DELETE /users/{userId}/roles/inexistente` | Falha por role não encontrado | HTTP 404 `ROLE_NOT_FOUND` |
| Requisição sem token | Chamada a qualquer endpoint de role-user sem Bearer token | Rejeição imediata de autenticação | HTTP 401 `INVALID_TOKEN` |

</frozen-after-approval>

## Code Map

- `src/Application/DTO/Role/AssignRoleDTO.php` -- DTO com os identificadores do papel (`roleId`, `name`)
- `src/Domain/Port/Inbound/AssignUserRoleUseCaseInterface.php` -- Porta inbound para atribuição de role a usuário
- `src/Domain/Port/Inbound/UnassignUserRoleUseCaseInterface.php` -- Porta inbound para revogação de role de usuário
- `src/Domain/Port/Outbound/KeycloakRolePortInterface.php` -- Estender porta outbound com `assignRoleToUser` e `removeRoleFromUser`
- `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php` -- Implementar chamadas REST ao endpoint `/admin/realms/{realm}/users/{userId}/role-mappings/realm`
- `src/Application/UseCase/Role/AssignUserRoleUseCase.php` -- Use case com validação e orquestração de atribuição
- `src/Application/UseCase/Role/UnassignUserRoleUseCase.php` -- Use case para orquestração de desassociação
- `src/Infrastructure/Http/Controller/RoleController.php` -- Adicionar ações `assignRole` e `unassignRole`
- `src/Infrastructure/OpenApi/OpenApiSpecificationBuilder.php` -- Documentação OpenAPI para `POST /users/{userId}/roles` e `DELETE /users/{userId}/roles/{roleId}` e schema `AssignRoleRequest`
- `config/services.yaml` -- Registro dos novos use cases no container de injeção de dependências do Symfony
- `tests/Unit/Application/UseCase/Role/AssignUserRoleUseCaseTest.php` -- Testes unitários de atribuição de papel
- `tests/Unit/Application/UseCase/Role/UnassignUserRoleUseCaseTest.php` -- Testes unitários de revogação de papel
- `tests/Unit/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapterTest.php` -- Testes unitários do adapter para role mappings
- `tests/Unit/Infrastructure/Http/Controller/RoleControllerTest.php` -- Testes unitários dos novos métodos do controller
- `tests/Unit/Infrastructure/OpenApi/OpenApiSpecificationBuilderTest.php` -- Testes da especificação OpenAPI para endpoints de role mapping

## Tasks & Acceptance

**Execution:**
- [x] `src/Application/DTO/Role/AssignRoleDTO.php` -- Criar DTO para payload de atribuição de papéis -- Modelagem tipada de entrada
- [x] `src/Domain/Port/Inbound/AssignUserRoleUseCaseInterface.php` -- Declarar contrato de atribuição de role -- Inbound port
- [x] `src/Domain/Port/Inbound/UnassignUserRoleUseCaseInterface.php` -- Declarar contrato de desassociação de role -- Inbound port
- [x] `src/Domain/Port/Outbound/KeycloakRolePortInterface.php` -- Estender porta outbound com `assignRoleToUser` e `removeRoleFromUser` -- Outbound port contract
- [x] `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php` -- Implementar chamadas HTTP à Admin API do Keycloak (`/users/{userId}/role-mappings/realm`) -- Keycloak adapter
- [x] `src/Application/UseCase/Role/AssignUserRoleUseCase.php` -- Implementar regras de validação e fluxo de atribuição -- Core use case
- [x] `src/Application/UseCase/Role/UnassignUserRoleUseCase.php` -- Implementar regras de desassociação de role -- Core use case
- [x] `config/services.yaml` -- Mapear interfaces dos use cases na injeção de dependências do Symfony -- DI wiring
- [x] `src/Infrastructure/Http/Controller/RoleController.php` -- Implementar ações `POST /users/{userId}/roles` e `DELETE /users/{userId}/roles/{roleId}` -- HTTP endpoints
- [x] `src/Infrastructure/OpenApi/OpenApiSpecificationBuilder.php` -- Documentar endpoints e schema `AssignRoleRequest` no Swagger/OpenAPI -- OpenAPI specification
- [x] `tests/Unit/Application/UseCase/Role/AssignUserRoleUseCaseTest.php` -- Implementar testes unitários para atribuição de role -- Unit testing
- [x] `tests/Unit/Application/UseCase/Role/UnassignUserRoleUseCaseTest.php` -- Implementar testes unitários para revogação de role -- Unit testing
- [x] `tests/Unit/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapterTest.php` -- Adicionar testes unitários para métodos de role mapping no adaptador -- Adapter testing
- [x] `tests/Unit/Infrastructure/Http/Controller/RoleControllerTest.php` -- Cobrir endpoints de role mapping em testes unitários -- Controller testing
- [x] `tests/Unit/Infrastructure/OpenApi/OpenApiSpecificationBuilderTest.php` -- Assegurar presença das novas rotas no OpenAPI -- OpenAPI testing

**Acceptance Criteria:**
- Given um administrador autenticado, when envia `POST /users/{userId}/roles` com `roleId` ou `name` válido, then a role é associada ao usuário no Keycloak e a API responde HTTP 200.
- Given uma requisição `POST /users/{userId}/roles` sem identificador de role, when a requisição é processada, then a API responde HTTP 400 `VALIDATION_ERROR`.
- Given um `userId` inexistente no Keycloak, when executado `POST /users/{userId}/roles`, then a API responde HTTP 404 `USER_NOT_FOUND`.
- Given um role inexistente ou logicamente inativado, when executado `POST /users/{userId}/roles`, then a API responde HTTP 404 `ROLE_NOT_FOUND`.
- Given um administrador autenticado, when envia `DELETE /users/{userId}/roles/{roleId}` para uma associação existente, then o papel é revogado do usuário no Keycloak e a API responde HTTP 204 No Content com corpo vazio.
- Given um `userId` ou `roleId` inexistente, when executado `DELETE /users/{userId}/roles/{roleId}`, then a API responde HTTP 404.
- Given acesso à especificação OpenAPI (`/docs/openapi.json`), when inspecionadas as rotas de role mapping, then `POST /users/{userId}/roles` e `DELETE /users/{userId}/roles/{roleId}` estão documentadas com seus respectivos schemas e códigos HTTP.

## Spec Change Log

_Nenhuma alteração registrada até o momento._

## Design Notes

O Keycloak requer que os payloads de atribuição e revogação de papéis em `/admin/realms/{realm}/users/{userId}/role-mappings/realm` sejam uma lista de representações de roles contendo no mínimo `id` e `name`:
```json
[
  {
    "id": "uuid-do-papel",
    "name": "nome-do-papel"
  }
]
```
O adaptador `KeycloakRoleAdapter` valida a existência do usuário via `/users/{userId}`, resolve o papel pelo seu identificador (ID ou nome), verifica se está ativo (`isRoleActive`) e efetua a chamada com o payload exigido.

## Verification

**Commands:**
- `./bin/phpunit` -- expected: OK (todos os testes unitários passando)
- `curl -s http://localhost:8181/docs/openapi.json | grep -E '"/users/\{userId\}/roles"'` -- expected: rota documentada

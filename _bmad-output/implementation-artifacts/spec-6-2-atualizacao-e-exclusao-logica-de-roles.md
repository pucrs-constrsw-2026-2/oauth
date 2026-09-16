---
title: 'Story 6.2: Atualização e Exclusão Lógica de Roles'
type: 'feature'
created: '2026-09-16'
status: 'done'
baseline_commit: 'ab21a37cefddc1c6df4700949271fa5dd888d693'
review_loop_iteration: 0
context:
  - '_bmad-output/implementation-artifacts/epic-6-context.md'
  - '_bmad-output/planning-artifacts/architecture.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O microserviço `oauth` não permite que administradores atualizem nomes e descrições de papéis institucionais nem realizem sua inativação segura, inviabilizando a evolução do ciclo de vida das Realm Roles sem perda de rastreabilidade.

**Approach:** Implementar os endpoints `PUT /roles/{id}` (atualização completa), `PATCH /roles/{id}` (atualização parcial) e `DELETE /roles/{id}` (exclusão lógica com `attributes.enabled = false`) no Keycloak, com validação de dados, isolamento em Arquitetura Hexagonal e documentação completa no Swagger/OpenAPI.

## Boundaries & Constraints

**Always:**
- Adoção estrita da Arquitetura Hexagonal: regras de negócio e validações na camada de aplicação/domínio, desacopladas do Symfony e do Keycloak.
- Exclusão estritamente lógica (NFR-6 e AD-8): requisições `DELETE /roles/{id}` inativam a role (`attributes.enabled = false`), preservando o registro físico no Keycloak e respondendo HTTP 204 No Content com corpo vazio.
- `GET /roles` e `GET /roles/{id}` continuam ignorando roles inativadas (retornando 404 para busca pontual de role inativada).
- Endpoints documentados no Swagger UI (`/docs`) e na rota OpenAPI (`/docs/openapi.json`).
- Todas as operações exigem Bearer token válido no header `Authorization`.

**Ask First:**
- Nenhuma decisão divergente identificada para esta estória.

**Never:**
- Nunca executar hard-delete no Keycloak (`DELETE admin/realms/{realm}/roles-by-id/{id}`).
- Nunca acoplar classes de framework ou de cliente HTTP na camada de domínio.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Atualização completa de role | `PUT /roles/{id}` com `{"name": "novo-nome", "description": "nova desc"}` | Role atualizada com sucesso no Keycloak | HTTP 200 com `RoleResponse` JSON |
| Atualização completa sem nome | `PUT /roles/{id}` com `{"description": "desc"}` ou `{"name": ""}` | Rejeição imediata por validação de campos obrigatórios | HTTP 400 `VALIDATION_ERROR` |
| Atualização parcial (PATCH) com descrição | `PATCH /roles/{id}` com `{"description": "Apenas nova desc"}` | Atualiza apenas descrição, mantendo nome inalterado | HTTP 200 com `RoleResponse` JSON |
| Atualização parcial (PATCH) com nome | `PATCH /roles/{id}` com `{"name": "novo-nome"}` | Atualiza apenas o nome, mantendo descrição inalterada | HTTP 200 com `RoleResponse` JSON |
| Atualização parcial (PATCH) vazia | `PATCH /roles/{id}` com `{}` ou campos em branco | Rejeição por ausência de dados válidos para alteração | HTTP 400 `VALIDATION_ERROR` |
| Role não encontrada em PUT/PATCH/DELETE | `PUT`, `PATCH` ou `DELETE /roles/inexistente` | Role não localizada ou inativada | HTTP 404 `ROLE_NOT_FOUND` |
| Exclusão lógica com sucesso | `DELETE /roles/{id}` para role ativa | Marcação de `attributes.enabled = false` no Keycloak | HTTP 204 No Content com corpo vazio |
| Consulta pós-exclusão lógica | `GET /roles/{id}` ou `GET /roles` para role inativada | Role inativada não deve ser retornada | HTTP 404 em `GET /roles/{id}` e ausente em `GET /roles` |

</frozen-after-approval>

## Code Map

- `src/Domain/Port/Outbound/KeycloakRolePortInterface.php` -- Interface da porta outbound para estender com `updateRole` e `disableRole`
- `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php` -- Adaptador Keycloak com as chamadas REST PUT para atualização e inativação lógica
- `src/Application/DTO/Role/UpdateRoleDTO.php` -- DTO com os atributos a serem atualizados (`name`, `description`)
- `src/Domain/Port/Inbound/UpdateRoleUseCaseInterface.php` -- Porta inbound para atualização (PUT e PATCH) de papéis
- `src/Domain/Port/Inbound/DeleteRoleUseCaseInterface.php` -- Porta inbound para exclusão lógica de papéis
- `src/Application/UseCase/Role/UpdateRoleUseCase.php` -- Use case com as regras de validação e persistência de alterações
- `src/Application/UseCase/Role/DeleteRoleUseCase.php` -- Use case para orquestrar a inativação lógica
- `src/Infrastructure/Http/Controller/RoleController.php` -- Controller HTTP adicionando as rotas `PUT /roles/{id}`, `PATCH /roles/{id}`, `DELETE /roles/{id}`
- `src/Infrastructure/OpenApi/OpenApiSpecificationBuilder.php` -- Documentação OpenAPI de `put`, `patch`, `delete` sob `/roles/{id}` e schemas
- `config/services.yaml` -- Registro e injeção de dependências dos novos use cases
- `tests/Unit/Application/UseCase/Role/UpdateRoleUseCaseTest.php` -- Testes unitários de atualização de role (PUT/PATCH)
- `tests/Unit/Application/UseCase/Role/DeleteRoleUseCaseTest.php` -- Testes unitários de exclusão lógica de role (DELETE)
- `tests/Unit/Infrastructure/Http/Controller/RoleControllerTest.php` -- Testes unitários do controller para PUT, PATCH e DELETE
- `tests/Unit/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapterTest.php` -- Testes unitários do adaptador Keycloak para update e disable
- `tests/Unit/Infrastructure/OpenApi/OpenApiSpecificationBuilderTest.php` -- Testes unitários da especificação Swagger/OpenAPI

## Tasks & Acceptance

**Execution:**
- [x] `src/Application/DTO/Role/UpdateRoleDTO.php` -- Criar DTO para transferência de dados de atualização de papéis -- Modelagem tipada de entrada
- [x] `src/Domain/Port/Inbound/UpdateRoleUseCaseInterface.php` -- Declarar contrato de atualização (completa e parcial) de role -- Inbound port
- [x] `src/Domain/Port/Inbound/DeleteRoleUseCaseInterface.php` -- Declarar contrato de inativação lógica de role -- Inbound port
- [x] `src/Domain/Port/Outbound/KeycloakRolePortInterface.php` -- Estender porta outbound com `updateRole` e `disableRole` -- Outbound port contract
- [x] `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php` -- Implementar chamadas HTTP à Admin API do Keycloak para atualizar e desativar role via PUT em `/roles-by-id/{id}` -- Keycloak adapter
- [x] `src/Application/UseCase/Role/UpdateRoleUseCase.php` -- Implementar regras de negócio, validações de PUT e PATCH e persistência -- Core use case
- [x] `src/Application/UseCase/Role/DeleteRoleUseCase.php` -- Implementar fluxo de soft delete e verificação de existência -- Core use case
- [x] `config/services.yaml` -- Mapear interfaces dos use cases para suas implementações na injeção de dependências do Symfony -- DI wiring
- [x] `src/Infrastructure/Http/Controller/RoleController.php` -- Adicionar ações `update`, `patch` e `delete` mapeadas para `/roles/{id}` -- HTTP endpoints
- [x] `src/Infrastructure/OpenApi/OpenApiSpecificationBuilder.php` -- Adicionar documentação OpenAPI para PUT, PATCH e DELETE em `/roles/{id}` e schemas `UpdateRoleRequest` e `PatchRoleRequest` -- Swagger specification
- [x] `tests/Unit/Application/UseCase/Role/UpdateRoleUseCaseTest.php` -- Implementar testes unitários para atualização de roles (PUT/PATCH) -- Unit testing
- [x] `tests/Unit/Application/UseCase/Role/DeleteRoleUseCaseTest.php` -- Implementar testes unitários para exclusão lógica de roles (DELETE) -- Unit testing
- [x] `tests/Unit/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapterTest.php` -- Atualizar testes unitários do adaptador cobrindo update e disable -- Adapter testing
- [x] `tests/Unit/Infrastructure/Http/Controller/RoleControllerTest.php` -- Cobrir novos métodos do controller em testes unitários -- Controller testing
- [x] `tests/Unit/Infrastructure/OpenApi/OpenApiSpecificationBuilderTest.php` -- Assegurar presença dos novos endpoints e schemas no OpenAPI -- OpenAPI testing

**Acceptance Criteria:**
- Given um administrador autenticado, when envia `PUT /roles/{id}` com `name` e `description` válidos, then a role é atualizada no Keycloak e retorna HTTP 200 com os dados atualizados.
- Given um payload de `PUT /roles/{id}` com `name` ausente ou vazio, when a requisição é processada, then a API responde HTTP 400 com erro de validação.
- Given um administrador autenticado, when envia `PATCH /roles/{id}` com campos parciais (somente `description` ou somente `name`), then a API atualiza apenas os campos fornecidos e responde HTTP 200 com os dados atualizados.
- Given uma requisição `PATCH /roles/{id}` sem nenhum campo fornecido, when a requisição é processada, then a API responde HTTP 400.
- Given um `id` de role inexistente ou já inativada, when executado `PUT`, `PATCH` ou `DELETE /roles/{id}`, then a API responde HTTP 404 `ROLE_NOT_FOUND`.
- Given um administrador autenticado, when envia `DELETE /roles/{id}` para uma role existente, then a API inativa a role no Keycloak com `attributes.enabled = false` e responde HTTP 204 com corpo vazio.
- Given uma role inativada por `DELETE /roles/{id}`, when subsequente `GET /roles` for executado, then o role não aparece na lista de papéis retornados.
- Given uma role inativada por `DELETE /roles/{id}`, when subsequente `GET /roles/{id}` for executado, then a API responde HTTP 404.
- Given acesso à especificação OpenAPI (`/docs/openapi.json`), when inspecionadas as rotas de `/roles/{id}`, then as operações `put`, `patch` e `delete` estão presentes com schemas e códigos de resposta documentados.

## Spec Change Log

_Nenhuma alteração registrada até o momento._

## Design Notes

A exclusão lógica de roles no Keycloak é realizada atualizando os atributos do papel:
```json
{
  "name": "gestor",
  "description": "...",
  "attributes": {
    "enabled": ["false"]
  }
}
```
Isso é feito via `PUT admin/realms/{realm}/roles-by-id/{id}`, sem invocar o método `DELETE` da API do Keycloak, garantindo o atendimento do requisito NFR-6 e da decisão AD-8.
Para tolerar buscas tanto por UUID do Keycloak quanto pelo nome do papel no parâmetro `{id}`, o adaptador primeiro pesquisa via endpoint `/roles-by-id/{id}` e, em caso de 404, realiza fallback pelo nome em `/roles/{name}`.

## Verification

**Commands:**
- `./bin/phpunit` -- expected: OK (todos os testes unitários e de integração passando)
- `curl -s http://localhost:8181/docs/openapi.json | grep -E '"put"|"patch"|"delete"'` -- expected: operações documentadas sob `/roles/{id}`

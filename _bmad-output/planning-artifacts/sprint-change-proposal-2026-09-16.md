---
title: Sprint Change Proposal — Gestão e Atribuição de Roles
created: 2026-09-16
status: approved
scope: moderate
---

# Sprint Change Proposal: Gestão de Ciclo de Vida de Roles e Atribuição a Usuários

## 1. Issue Summary

Nova demanda funcional apresentada para o microserviço de autenticação e autorização (`oauth`): expandir a API REST para suportar a administração centralizada de papéis (Roles) e o gerenciamento das associações de papéis a usuários (Role Mapping) sobre o Keycloak.

Endpoints solicitados:
- `POST {{base-api-url}}/roles`: criação de um role
- `GET {{base-api-url}}/roles`: recuperação dos dados de todos os roles
- `GET {{base-api-url}}/roles/{{id}}`: recuperação de um role pelo id
- `PUT {{base-api-url}}/roles/{{id}}`: atualização integral de um role
- `PATCH {{base-api-url}}/roles/{{id}}`: atualização parcial de um role
- `DELETE {{base-api-url}}/roles/{{id}}`: exclusão lógica de um role
- `POST {{base-api-url}}/users/{{userId}}/roles`: atribuição de um role a um usuário
- `DELETE {{base-api-url}}/users/{{userId}}/roles/{{roleId}}`: desatribuição/revogação de um role de um usuário

Conforme solicitação do usuário, a entrega dessas novas funcionalidades deve ser estruturada e dividida em **3 partes (histórias)** para viabilizar execução cadenciada e testes modulares.

## 2. Impact Analysis

- **Epic Impact:** Os épicos 1 a 4 permanecem `done` e intocados. O épico 5 (Observabilidade/Grafana) segue seu fluxo independente. É criado o novo **Epic 6: Gestão do Ciclo de Vida de Roles e Atribuição de Papéis a Usuários**, dividido em 3 histórias (6.1, 6.2 e 6.3).
- **Story Impact:** Três novas histórias adicionadas ao backlog da sprint:
  - **Story 6.1:** Criação e Consulta de Roles (endpoints `POST /roles`, `GET /roles`, `GET /roles/{id}`).
  - **Story 6.2:** Atualização e Exclusão Lógica de Roles (endpoints `PUT /roles/{id}`, `PATCH /roles/{id}`, `DELETE /roles/{id}`).
  - **Story 6.3:** Atribuição e Desassociação de Roles a Usuários (endpoints `POST /users/{userId}/roles`, `DELETE /users/{userId}/roles/{roleId}`).
- **Artifact Conflicts & Updates:**
  - **PRD:** Adicionados novos Requisitos Funcionais FR-11 a FR-18 na nova seção 4.5 ("Gestão e Atribuição de Roles"), novas jornadas de usuário UJ-5 e UJ-6, e atualização das métricas de sucesso.
  - **Arquitetura:** Nova decisão arquitetural AD-8 detalhando o mapeamento de Realm Roles e Role-Mappings no Keycloak Admin API, modelo de exclusão lógica de papéis via atributo de estado (`attributes.enabled = false`), novos contratos de portas inbound/outbound e mapeamento de capacidades no Spine.
  - **Epics:** Novo Epic 6 detalhado com critérios de aceitação no padrão BDD (Given/When/Then) para as 3 histórias.
  - **Sprint Status:** Registro de `epic-6` e histórias `6-1`, `6-2` e `6-3` em estado `backlog`.
- **Technical Impact:**
  - Criação de novos DTOs em `App\Application\DTO\Role\`.
  - Novas interfaces de portas inbound em `App\Domain\Port\Inbound\` para cada caso de uso.
  - Nova interface outbound `KeycloakRolePortInterface` (ou expansão correlata) em `App\Domain\Port\Outbound\`.
  - Novo adaptador de infraestrutura `KeycloakRoleAdapter` consumindo a Admin API do Keycloak (`/admin/realms/{realm}/roles`, `/roles-by-id/{id}` e `/users/{userId}/role-mappings/realm`).
  - Novos controllers HTTP: `RoleController` (para rotas `/roles`) e integração de rotas de relacionamento em `UserRoleController` ou `UserController`.
  - Exceções de domínio específicas: `RoleNotFoundException`, `RoleAlreadyExistsException`.

## 3. Recommended Approach

**Direct Adjustment** com adição do **Epic 6**, subdividido nas 3 partes solicitadas:
- **Parte 1 (Story 6.1):** Criação e Consulta de Roles (Base, Portas, Leitura e Criação).
- **Parte 2 (Story 6.2):** Modificação e Exclusão Lógica de Roles (Alterações completas, parciais e soft-delete).
- **Parte 3 (Story 6.3):** Atribuição e Desassociação de Roles a Usuários (Mapeamento de papéis no Keycloak).

Essa divisão mantém a arquitetura hexagonal intacta, respeita os princípios de responsabilidade única (SRP), não afeta os 4 épicos originais já homologados e permite desenvolvimento e revisão isolados.

## 4. Detailed Change Proposals

| Artefato | Mudança Realizada | Arquivo(s) |
|---|---|---|
| **PRD** | Adicionados FR-11 a FR-18, UJ-5, UJ-6 e atualização de SM-1 | `_bmad-output/planning-artifacts/prd.md`, `_bmad-output/planning-artifacts/prds/prd-oauth-2026-09-02/prd.md` |
| **Arquitetura** | Decisão AD-8, expansão da estrutura de diretórios e tabela Capability Map | `_bmad-output/planning-artifacts/architecture.md`, `_bmad-output/planning-artifacts/architecture/architecture-oauth-2026-09-02/ARCHITECTURE-SPINE.md` |
| **Epics** | Inclusão do Epic 6 com Stories 6.1, 6.2 e 6.3 e critérios de aceitação BDD | `_bmad-output/planning-artifacts/epics.md` |
| **Sprint Status** | Adicionado `epic-6` e histórias `6-1`, `6-2`, `6-3` como `backlog` | `_bmad-output/implementation-artifacts/sprint-status.yaml` |

## 5. Implementation Handoff

- **Classificação de Escopo:** Moderate (novos contratos de portas, novos casos de uso e integração com endpoints de roles e role-mapping da Admin API do Keycloak).
- **Responsável:** Desenvolvedor backend.
- **Branch recomendada:** `grupo01/feat/roles-management` (ou subdividida por partes conforme fluxo do time).
- **Próximos Passos de Execução:** Iniciar a implementação pela Story 6.1 (fundação dos DTOs, interfaces de portas de role, criação e consultas).

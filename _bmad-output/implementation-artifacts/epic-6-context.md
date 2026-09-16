# Epic 6 Context: Gestão do Ciclo de Vida de Roles e Atribuição de Papéis a Usuários

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Permitir a administração centralizada de papéis institucionais (roles) e o gerenciamento dinâmico de atribuições de papéis a usuários sobre o Keycloak através de uma API REST desacoplada, seguindo estritamente a Arquitetura Hexagonal e preservando a integridade histórica de dados.

## Stories

- Story 6.1: Criação e Consulta de Roles (Parte 1)
- Story 6.2: Atualização e Exclusão Lógica de Roles (Parte 2)
- Story 6.3: Atribuição e Desassociação de Roles a Usuários (Parte 3)

## Requirements & Constraints

- Todas as rotas de roles e mapeamento exigem autenticação via Bearer token válido (`Authorization: Bearer <token>`). Tokens ausentes, expirados ou inválidos retornam HTTP 401 Unauthorized.
- Operações de administração de papéis operam sobre as Realm Roles do Keycloak.
- Exclusão de papéis é estritamente lógica (soft delete via atributo de estado inativo), impedindo a remoção irreversível no banco de identidades e filtrando papéis inativos das listagens padrão (`GET /roles`).
- Mapeamento de papéis em usuários atualiza diretamente o Keycloak Role Mappings (`role-mappings/realm`), refletindo de imediato nos novos tokens emitidos.
- Respostas de erro devem seguir o envelope padrão JSON (`error.code`, `error.message`, `error.details`).
- Regra de isolamento de domínio: a camada `Domain` permanece 100% agnóstica a frameworks e bibliotecas externas do Keycloak.

## Technical Decisions

- **AD-8 — Realm Roles e Role Mappings via Admin API:** A comunicação com o Keycloak utiliza o cliente HTTP administrativo autenticado via Service Account (`client_credentials` do client `oauth`).
- **Identificação por ID:** Consultas, atualizações e exclusões pontuais utilizam a rota por ID do Keycloak (`/admin/realms/{realm}/roles-by-id/{id}`).
- **Portas & Adaptadores:**
  - `src/Domain/Port/Inbound/`: Interfaces para cada caso de uso (`CreateRoleUseCaseInterface`, `ListRolesUseCaseInterface`, `GetRoleByIdUseCaseInterface`, etc.).
  - `src/Domain/Port/Outbound/KeycloakRolePortInterface.php`: Porta outbound para operações de roles no Keycloak.
  - `src/Infrastructure/Keycloak/Adapter/KeycloakRoleAdapter.php`: Adaptador concreto implementando a porta outbound via `KeycloakHttpClient`.
  - `src/Infrastructure/Http/Controller/RoleController.php`: Controlador REST expondo os endpoints `/roles` e rotas de mapeamento.
- **DTOs & Modelos:** DTOs dedicados em `App\Application\DTO\Role\` e modelo de domínio em `App\Domain\Model\Role.php` (ou representação de entidade desacoplada).

## Cross-Story Dependencies

- **Story 6.1** é pré-requisito funcional e estrutural: estabelece a porta outbound `KeycloakRolePortInterface`, o modelo/DTO base de Role e as rotas de criação e busca.
- **Story 6.2** expande a porta e o adaptador com métodos de atualização integral, parcial e inativação lógica.
- **Story 6.3** integra as Realm Roles de Story 6.1 com o ciclo de vida de usuários do Epic 3 via API de Role Mappings do Keycloak.

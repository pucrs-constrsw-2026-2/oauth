# Addendum Técnico: Decisões de Arquitetura e Implementação (OAuth Microservice)

Este adendo registra decisões de design, detalhes técnicos de transporte e mapeamentos de infraestrutura capturados durante a elaboração do PRD, destinados a alimentar o workflow de Arquitetura (`bmad-architecture`) e a implementação da base.

---

## 1. Mapeamento de Configuração do Keycloak

### 1.1 Realm e Client
- **Realm:** `constrsw`
- **Configuração de Login:** Email como username (`Registration with email as username`).
- **Client ID:** `oauth`
- **Access Type:** Confidencial (`confidential`).
- **Service Accounts Enabled:** `true` (para permitir Client Credentials grant da aplicação na Admin API).
- **Client Secret:** Gerado e injetado via variável de ambiente (`KEYCLOAK_CLIENT_SECRET`).

### 1.2 Matriz de Roles, Resources, Policies e Permissions
- **Roles:**
  - `administrator`
  - `coordinator`
  - `professor`
  - `student`

- **Resources (com URL base):**
  - `classes` (ex: `/classes`)
  - `courses` (ex: `/courses`)
  - `lessons` (ex: `/lessons`)
  - `professors` (ex: `/professors`)
  - `reservations` (ex: `/reservations`)
  - `resources` (ex: `/resources`)
  - `rooms` (ex: `/rooms`)
  - `students` (ex: `/students`)

- **Policies (Tipo: Role):**
  - `administrator-policy` (Role: `administrator`, filtrado pelo client `oauth`)
  - `coordinator-policy` (Role: `coordinator`, filtrado pelo client `oauth`)
  - `professor-policy` (Role: `professor`, filtrado pelo client `oauth`)

- **Permissions (Tipo: Resource-based):**
  - `administrator-permissions`:
    - Associado aos Resources: `resources`, `rooms`, `professors`, `students`
    - Associado à Policy: `administrator-policy`
  - `coordinator-permissions`:
    - Associado aos Resources: `courses`, `classes`
    - Associado à Policy: `coordinator-policy`
  - `professor-permissions`:
    - Associado aos Resources: `lessons`, `reservations`
    - Associado à Policy: `professor-policy`

---

## 2. Esboço das Portas da Arquitetura Hexagonal

A camada de Domínio (`src/Domain`) expõe contratos agnósticos para que os adaptadores da camada de Infraestrutura (`src/Infrastructure`) façam a integração com o Keycloak:

```php
namespace App\Domain\Port\Inbound; // Use Cases
namespace App\Domain\Port\Outbound; // Interfaces de infraestrutura

// Outbound Ports (Adaptadores Keycloak)
interface AuthPortInterface
{
    public function authenticate(string $username, string $password): AuthTokensDTO;
    public function refreshToken(string $refreshToken): AuthTokensDTO;
    public function getUserInfo(string $accessToken): UserProfileDTO;
}

interface UserManagementPortInterface
{
    public function createUser(CreateUserDTO $dto): UserDTO;
    public function listActiveUsers(): array;
    public function getUserById(string $id): ?UserDTO;
    public function updateUser(string $id, UpdateUserDTO $dto): void;
    public function updatePassword(string $id, string $newPassword): void;
    public function disableUser(string $id): void;
}

interface AuthorizationPortInterface
{
    public function validateAccess(string $accessToken, string $resource): bool;
}
```

---

## 3. Topologia Docker Compose

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:22.0.5 # ou jboss/keycloak com /auth/
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: password123
      KC_HTTP_RELATIVE_PATH: /auth
    ports:
      - "8080:8080"
    volumes:
      - ./docker/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro
    command: start-dev --import-realm

  app:
    build:
      context: .
      dockerfile: docker/php/Dockerfile
    environment:
      KEYCLOAK_BASE_URL: http://keycloak:8080/auth
      KEYCLOAK_REALM: constrsw
      KEYCLOAK_CLIENT_ID: oauth
      KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_CLIENT_SECRET}
    volumes:
      - .:/var/www/html

  nginx:
    image: nginx:alpine
    ports:
      - "8000:80"
    volumes:
      - .:/var/www/html
      - ./docker/nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - app
```

---

## 4. Estratégia de Branches para o Grupo (4 Integrantes)

1. **`main` (Sua Responsabilidade - Fundação):**
   - Configuração do Docker Compose + import do Keycloak.
   - Esqueleto Hexagonal (`src/Domain`, `src/Application`, `src/Infrastructure`).
   - Declaração de todas as Interfaces de Portas e DTOs comuns.
   - Handlers globais de exceção.
2. **`feature/auth-tokens` (Integrante 2):**
   - Implementação do `KeycloakAuthAdapter` + Use Cases de Login e Refresh.
   - Endpoints: `POST /login`, `POST /refresh`, `GET /me`.
3. **`feature/user-management` (Integrante 3):**
   - Implementação do `KeycloakUserManagementAdapter` + Use Cases de CRUD.
   - Endpoints: `POST /users`, `GET /users`, `GET /users/{id}`, `PUT /users/{id}`, `PATCH /users/{id}`, `DELETE /users/{id}`.
4. **`feature/authorization-policies` (Integrante 4):**
   - Implementação do `KeycloakAuthorizationAdapter` + Use Case de verificação de permissões.
   - Endpoint: `POST /authorize`.

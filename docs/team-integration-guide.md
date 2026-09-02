# Guia de Desenvolvimento e Integração — Grupo 01

**Projeto:** Microserviço de Autenticação e Autorização (OAuth 2.0 / OIDC com Keycloak)  
**Disciplina:** Construção de Software (2026/2)  
**Time:** Grupo 01  
**Integrantes:** Fernando Gazzana, Luiz Agusto Kubaszewski, Giancarlo Mena, Vinicius Silva  
**Branch Base do Grupo:** `grupo01` *(nossa "main")*  
**Arquitetura:** Hexagonal (*Ports & Adapters*)  

---

> [!IMPORTANT]
> **Contexto de Git e Submódulo:**
> Este repositório é um submódulo compartilhado por **toda a turma**. Cada grupo possui sua própria branch principal.
> A branch base do nosso time é **`grupo01`**. 
> **NUNCA** faça commits ou abra Pull Requests para a branch `main` do repositório da turma! Todo o trabalho do nosso grupo deve partir de `grupo01` e voltar via PR para `grupo01`.

---

## 1. Visão Geral e Arquitetura

Este microserviço é uma API REST desenvolvida em **PHP com Symfony** que atua como fachada para o **Keycloak**. Os demais sistemas da faculdade se comunicarão com a nossa API, sem nunca acessar o Keycloak diretamente.

Adotamos a **Arquitetura Hexagonal**:
* **`src/Domain` (Núcleo Puro):** Contém as entidades e as **Interfaces de Portas (*Ports*)**. Não depende de NADA externo (nem de Symfony, nem de Keycloak).
* **`src/Application` (Casos de Uso):** Contém os Use Cases e DTOs que orquestram a regra de negócio.
* **`src/Infrastructure` (Adaptadores):** Contém o código que fala com a API do Keycloak e o Nginx/Symfony.
* **`src/Infrastructure/Http/Controller`:** Recebe as requisições HTTP e devolve JSON.

---

## 2. Como Rodar o Projeto Localmente (Docker)

Nenhum integrante precisa instalar PHP ou Keycloak na sua máquina física. Tudo roda via Docker.

### Passo a passo:
1. Garanta que você está na branch base do nosso grupo (`grupo01`) atualizada:
   ```bash
   git checkout grupo01
   git pull origin grupo01
   ```
2. Inicie os containers com o Docker Compose:
   ```bash
   docker compose up -d
   ```
3. O ambiente subirá automaticamente:
   * **API REST (Symfony):** `http://localhost:8000`
   * **Keycloak:** `http://localhost:8080/auth` (ou `http://localhost:8080`)
     * **Admin User:** `admin` | **Admin Password:** `password123`
     * **Realm pré-carregado:** `constrsw`
     * **Client ID:** `oauth` (Client Secret já configurado)
     * *Observação:* O Keycloak já sobe com o realm, client, roles, resources e policies configurados automaticamente! Não é necessário criar nada manualmente pela interface do Keycloak.

---

## 3. As Três Regras de Ouro do Time ⚠️

Para garantir que todos consigam trabalhar simultaneamente sem quebrar o código de ninguém e sem conflitos de merge:

1. **NUNCA altere as assinaturas das Interfaces em `src/Domain/Port/`:** Elas são o contrato compartilhado. Se você mudar o nome de um método ou tipo de retorno, o código dos outros 3 colegas quebrará.
2. **NUNCA faça chamadas ao Keycloak fora de `src/Infrastructure/Keycloak/`:** A camada de aplicação e os controllers não devem conhecer URLs ou detalhes de HTTP do Keycloak.
3. **Respeite a sua pasta de trabalho:** Cada integrante possui seus próprios Use Cases, seu próprio Adaptador e seu próprio Controller.

---

## 4. Divisão de Tarefas e Branches do Grupo 01

O trabalho foi dividido em **3 frentes independentes**. Cada integrante deve abrir sua branch a partir de `grupo01` seguindo o padrão de nomenclatura:

---

### 👤 Integrante 2: Fluxo de Autenticação e Tokens
* **Branch de trabalho:**
  ```bash
  git checkout grupo01
  git pull origin grupo01
  git checkout -b grupo01/feat/auth-tokens
  ```
* **Endpoints a implementar:**
  1. `POST /login`: Recebe `username` e `password`, aciona o Keycloak e devolve `access_token`, `refresh_token`, `expires_in`.
  2. `POST /refresh`: Recebe `refresh_token`, aciona o Keycloak e renova a sessão.
  3. `GET /me` (ou `/userinfo`): Recebe o Bearer Token no header e devolve os dados do perfil do usuário logado.
* **Arquivos de sua responsabilidade exclusiva:**
  * `src/Application/UseCase/Auth/LoginUseCase.php`
  * `src/Application/UseCase/Auth/RefreshTokenUseCase.php`
  * `src/Application/UseCase/Auth/GetUserInfoUseCase.php`
  * `src/Infrastructure/Keycloak/Adapter/KeycloakAuthAdapter.php` (implementando `KeycloakAuthPortInterface`)
  * `src/Infrastructure/Http/Controller/AuthController.php`
* **Status HTTP esperados:**
  * Sucesso: `200 OK`
  * Credenciais ou token inválido/expirado: `401 Unauthorized`
  * Dados obrigatórios ausentes: `400 Bad Request`

---

### 👤 Integrante 3: Gestão de Usuários (User Management CRUD)
* **Branch de trabalho:**
  ```bash
  git checkout grupo01
  git pull origin grupo01
  git checkout -b grupo01/feat/user-management
  ```
* **Endpoints a implementar:**
  1. `POST /users`: Cria um novo usuário no Keycloak. Retorna `201 Created` com o ID do usuário criado.
  2. `GET /users`: Retorna a lista de todos os usuários com status ativo (`enabled: true`). Retorna `200 OK`.
  3. `GET /users/{id}`: Busca um usuário específico pelo ID. Retorna `200 OK` ou `404 Not Found`.
  4. `PUT /users/{id}`: Atualiza os dados de um usuário existente. Retorna `200 OK` ou `404 Not Found`.
  5. `PATCH /users/{id}`: Atualiza a senha de um usuário (`password`). Retorna `200 OK` ou `404 Not Found`.
  6. `DELETE /users/{id}`: **Exclusão Lógica!** Deve desabilitar o usuário (`enabled: false`) no Keycloak. Retorna `204 No Content` ou `404 Not Found`.
* **Arquivos de sua responsabilidade exclusiva:**
  * `src/Application/UseCase/User/*UseCase.php`
  * `src/Infrastructure/Keycloak/Adapter/KeycloakUserAdapter.php` (implementando `KeycloakUserPortInterface`)
  * `src/Infrastructure/Http/Controller/UserController.php`
* **Dica de Implementação:** Para chamar a Admin API do Keycloak (`/admin/realms/constrsw/users`), utilize a classe base `KeycloakHttpClient`, que já gerencia automaticamente a autenticação de serviço com o client secret.

---

### 👤 Integrante 4: Autorização e Avaliação de Políticas
* **Branch de trabalho:**
  ```bash
  git checkout grupo01
  git pull origin grupo01
  git checkout -b grupo01/feat/authorization-policies
  ```
* **Endpoint a implementar:**
  * `POST /authorize`: Recebe o header `Authorization: Bearer <token>` e no corpo JSON:
    ```json
    {
      "resource": "rooms"
    }
    ```
* **Regra de Negócio (Matriz de Acesso do Professor):**
  A rota deve verificar os roles contidos no token do usuário e checar se ele tem permissão de acesso ao recurso solicitado:
  * Papel `administrator`: pode acessar `resources`, `rooms`, `professors`, `students`.
  * Papel `coordinator`: pode acessar `courses`, `classes`.
  * Papel `professor`: pode acessar `lessons`, `reservations`.
* **Status HTTP esperados:**
  * Acesso permitido: Retornar **`200 OK`**
  * Acesso negado (papel não tem permissão para o recurso): Retornar **`403 Forbidden`**
  * Token expirado ou inválido: Retornar **`401 Unauthorized`**
  * Recurso inexistente ou body inválido: Retornar **`400 Bad Request`**
* **Arquivos de sua responsabilidade exclusiva:**
  * `src/Application/UseCase/Authorization/AuthorizeResourceUseCase.php`
  * `src/Infrastructure/Keycloak/Adapter/KeycloakAuthorizationAdapter.php` (implementando `KeycloakAuthorizationPortInterface`)
  * `src/Infrastructure/Http/Controller/AuthorizationController.php`

---

## 5. Como Entregar o seu Código (Pull Request)

Quando finalizar a implementação da sua frente:
1. Verifique se o Docker sobe limpo e seus endpoints respondem aos status codes corretos.
2. Faça commit apenas dos arquivos da sua pasta:
   ```bash
   git add src/Application/UseCase/<SuaFrente>/
   git add src/Infrastructure/Keycloak/Adapter/<SeuAdapter>.php
   git add src/Infrastructure/Http/Controller/<SeuController>.php
   git commit -m "feat(<sua-frente>): implementa use cases, adapter e controller"
   ```
3. Suba sua branch para o repositório remoto:
   ```bash
   git push origin grupo01/feat/<sua-frente>
   ```
4. **Abra o Pull Request:**
   * **Base branch (Alvo):** `grupo01` *(NÃO selecione a branch `main`!)*
   * **Compare branch:** `grupo01/feat/<sua-frente>`
   * Como ninguém alterou os arquivos uns dos outros, o merge ocorrerá sem nenhum conflito!

# Descrição do Trabalho 1 — API OAuth / Keycloak

> **Disciplina:** Construção de Software (2026-2)  
> **Objetivo:** Desenvolver uma API REST que atue como *adapter* consumindo a API REST do Keycloak para gerenciamento de identidade, autenticação e autorização (usuários e roles).

---

## Sumário

- [1. Requisitos e Preparação do Ambiente](#1-requisitos-e-preparação-do-ambiente)
  - [1.1 Postman](#11-postman)
  - [1.2 Passo a Passo de Configuração](#12-passo-a-passo-de-configuração)
- [2. Rotas de Usuários (USERS)](#2-rotas-de-usuários-users)
  - [2.1 POST `/login`](#21-post-login)
  - [2.2 POST `/users`](#22-post-users)
  - [2.3 GET `/users`](#23-get-users)
  - [2.4 GET `/users/{id}`](#24-get-usersid)
  - [2.5 PUT `/users/{id}`](#25-put-usersid)
  - [2.6 PATCH `/users/{id}`](#26-patch-usersid)
  - [2.7 DELETE `/users/{id}`](#27-delete-usersid)
- [3. Rotas de Perfis/Papéis (ROLES)](#3-rotas-de-perfispapéis-roles)
- [4. Padrão de Tratamento de Erros](#4-padrão-de-tratamento-de-erros)
- [5. Material a ser Entregue](#5-material-a-ser-entregue)

---

## 1. Requisitos e Preparação do Ambiente

### 1.1 Postman
- Utilizem o **Postman** para fazer o acesso exploratório às APIs do Keycloak.
- Acessem o Postman via web ou baixem e instalem localmente, façam login e acessem a documentação Postman do Keycloak.

### 1.2 Passo a Passo de Configuração
1. **Repositório Base:** No repositório base, faça o checkout da branch do seu grupo.
2. **Container Keycloak:** O `docker-compose.yml` presente na raiz já executa um container do Keycloak.
3. **Backup e Cargas:** Ao ser executado, esse container restaura um backup do Keycloak que já configura:
   - Um realm: `constrsw`
   - Um client: `oauth`
4. **Variáveis de Ambiente:** Os parâmetros de acesso a essa instância do Keycloak já estão definidos no arquivo `.env` associado ao `docker-compose.yml`.
5. **Habilitar Serviço OAuth:** Abra o `docker-compose.yml` e descomente o serviço `oauth`.
6. **Repositório do Submódulo:** Vá para o repositório `oauth` (submódulo do base montado em `/backend/oauth`) e faça o checkout da branch do seu grupo.
7. **Dockerfile:** Comece criando o `Dockerfile` do seu projeto no repositório `oauth`.
8. **Desenvolvimento:** Desenvolva uma API REST consumindo a API REST do Keycloak implementando, no mínimo, as rotas detalhadas a seguir.

---

## 2. Rotas de Usuários (USERS)

### 2.1 POST `/login`
**Descrição:** Autenticação de usuário.

* **Endpoint:** `POST {{base-api-url}}/login`
* **Headers:** *(Vazio)*
* **Request Body:** `multipart/form-data` ou `application/x-www-form-urlencoded`
  ```
  username
  password
  ```
* **Response Body (`application/json`):**
  ```json
  {
    "token_type": "Bearer",
    "access_token": "...",
    "expires_in": 300,
    "refresh_token": "...",
    "refresh_expires_in": 1800
  }
  ```
* **Lógica:**
  Consumir a rota `POST {{base-keycloak-url}}/auth/realms/{{realm}}/protocol/openid-connect/token` da REST API do Keycloak para autenticação de usuário, gerando o `access_token` a partir de:
  - `client_id`
  - `client_secret`
  - `username`
  - `password`
  - `grant_type: password`
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `201` | Created | Autenticação bem-sucedida e tokens gerados |
  | `400` | Bad Request | Erro na estrutura da chamada (headers, request body etc.) |
  | `401` | Unauthorized | `username` e/ou `password` inválidos |

---

### 2.2 POST `/users`
**Descrição:** Criação de um novo usuário.

* **Endpoint:** `POST {{base-api-url}}/users`
* **Headers:**
  ```http
  Authorization: Bearer {{access_token}}
  Content-Type: application/json
  ```
* **Request Body (`application/json`):**
  ```json
  {
    "username": "usuario@exemplo.com",
    "password": "senhaPlainText",
    "first-name": "Nome",
    "last-name": "Sobrenome"
  }
  ```
  > **Nota:** `username` deve ser igual ao e-mail. Por enquanto, a senha pode ser enviada em texto plano (*plain text*).
* **Response Body (`application/json`):**
  ```json
  {
    "id": "uuid-do-usuario",
    "username": "usuario@exemplo.com",
    "first-name": "Nome",
    "last-name": "Sobrenome",
    "enabled": true
  }
  ```
  > O campo `id` é obtido a partir do header `Location` da resposta do Keycloak.
* **Lógica:** Consumir a rota do Keycloak responsável pela criação de novo usuário.
* **Validação de E-mail (RFC 5322):**
  O e-mail deve ser validado com a seguinte expressão regular oficial:
  ```regex
  ([-!#-'*+/-9=?A-Z^-~]+(\.[-!#-'*+/-9=?A-Z^-~]+)*|"([]!#-[^-~ \t]|(\\(\t -~]))+")@([-!#-'*+/-9=?A-Z^-~]+(\.[-!#-'*+/-9=?A-Z^-~]+)*|\[[\t -Z^-~]*\])
  ```
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `201` | Created | Usuário criado com sucesso |
  | `400` | Bad Request | Erro na estrutura da chamada ou e-mail inválido segundo o padrão RFC 5322 |
  | `401` | Unauthorized | Access token inválido |
  | `403` | Forbidden | Access token não concede permissão para acessar esse endpoint |
  | `409` | Conflict | `username` já existente |

---

### 2.3 GET `/users`
**Descrição:** Recuperação dos dados de todos os usuários cadastrados.

* **Endpoint:** `GET {{base-api-url}}/users`
* **Query Parameters:**
  - `enabled`: `true` ou `false` *(opcional)*  
    *Exemplo:* `GET {{base-api-url}}/users?enabled=true`
* **Headers:**
  ```http
  Authorization: Bearer {{access_token}}
  ```
* **Request Body:** *(Vazio)*
* **Response Body (`application/json`):**
  ```json
  [
    {
      "id": "uuid-do-usuario",
      "username": "usuario@exemplo.com",
      "first-name": "Nome",
      "last-name": "Sobrenome",
      "enabled": true
    }
  ]
  ```
* **Lógica:** Consumir a rota do Keycloak que recupera todos os usuários, aplicando os filtros informados.
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `200` | OK | Lista de usuários retornada com sucesso |
  | `400` | Bad Request | Erro na estrutura do request (headers, query params etc.) |
  | `401` | Unauthorized | Access token inválido |
  | `403` | Forbidden | Access token não concede permissão para acessar esse endpoint ou objeto |

---

### 2.4 GET `/users/{id}`
**Descrição:** Recuperação dos dados de um usuário específico por ID.

* **Endpoint:** `GET {{base-api-url}}/users/{{id}}`
* **Headers:**
  ```http
  Authorization: Bearer {{access_token}}
  ```
* **Request Body:** *(Vazio)*
* **Response Body (`application/json`):**
  ```json
  {
    "id": "uuid-do-usuario",
    "username": "usuario@exemplo.com",
    "first-name": "Nome",
    "last-name": "Sobrenome",
    "enabled": true
  }
  ```
* **Lógica:** Consumir a rota do Keycloak que recupera um usuário a partir do seu `id`.
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `200` | OK | Usuário localizado com sucesso |
  | `400` | Bad Request | Erro na estrutura da chamada (headers, parâmetros etc.) |
  | `401` | Unauthorized | Access token inválido |
  | `403` | Forbidden | Access token não concede permissão para acessar esse endpoint ou objeto |
  | `404` | Not Found | Objeto/usuário não localizado |

---

### 2.5 PUT `/users/{id}`
**Descrição:** Atualização dos dados cadastrais de um usuário.

* **Endpoint:** `PUT {{base-api-url}}/users/{{id}}`
* **Headers:**
  ```http
  Authorization: Bearer {{access_token}}
  Content-Type: application/json
  ```
* **Request Body (`application/json`):**
  Documento JSON representando os novos valores dos atributos do usuário (ex: `first-name`, `last-name`, etc.).
* **Response Body:** *(Vazio)*
* **Lógica:** Consumir a rota do Keycloak que atualiza um usuário via método `PUT`.
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `200` | OK | Usuário atualizado com sucesso |
  | `400` | Bad Request | Erro na estrutura da chamada (headers, request body etc.) |
  | `401` | Unauthorized | Access token inválido |
  | `403` | Forbidden | Access token não concede permissão para acessar esse endpoint ou objeto |
  | `404` | Not Found | Objeto/usuário não localizado |

---

### 2.6 PATCH `/users/{id}`
**Descrição:** Atualização da senha de um usuário.

* **Endpoint:** `PATCH {{base-api-url}}/users/{{id}}`
* **Headers:**
  ```http
  Authorization: Bearer {{access_token}}
  Content-Type: application/json
  ```
* **Request Body (`application/json`):**
  Documento JSON representando o novo valor do atributo `"password"`:
  ```json
  {
    "password": "novaSenha"
  }
  ```
* **Response Body:** *(Vazio)*
* **Lógica:** Consumir a rota do Keycloak que atualiza as credenciais/senha de um usuário (método `PATCH` ou reset-password).
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `200` | OK | Senha atualizada com sucesso |
  | `400` | Bad Request | Erro na estrutura da chamada (headers, request body etc.) |
  | `401` | Unauthorized | Access token inválido |
  | `403` | Forbidden | Access token não concede permissão para acessar esse endpoint ou objeto |
  | `404` | Not Found | Objeto/usuário não localizado |

---

### 2.7 DELETE `/users/{id}`
**Descrição:** Exclusão lógica de um usuário (desabilitação).

* **Endpoint:** `DELETE {{base-api-url}}/users/{{id}}`
* **Headers:**
  ```http
  Authorization: Bearer {{access_token}}
  ```
* **Request Body:** *(Vazio)*
* **Response Body:** *(Vazio)*
* **Lógica:** Desabilitar o usuário (`enabled = false`) consumindo a rota correspondente da API do Keycloak.
* **Response Codes:**
  | Código | Status | Descrição |
  |---|---|---|
  | `204` | No Content | Usuário desabilitado logicamente com sucesso |
  | `400` | Bad Request | Erro na estrutura da chamada (headers, parâmetros etc.) |
  | `401` | Unauthorized | Access token inválido |
  | `403` | Forbidden | Access token não concede permissão para acessar esse endpoint ou objeto |
  | `404` | Not Found | Objeto/usuário não localizado |

---

## 3. Rotas de Perfis/Papéis (ROLES)

Implementar as seguintes rotas para gerenciamento de papéis (roles):

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `{{base-api-url}}/roles` | Criação de um role |
| `GET` | `{{base-api-url}}/roles` | Recuperação dos dados de todos os roles |
| `GET` | `{{base-api-url}}/roles/{{id}}` | Recuperação de um role pelo `id` |
| `PUT` | `{{base-api-url}}/roles/{{id}}` | Atualização completa de um role |
| `PATCH` | `{{base-api-url}}/roles/{{id}}` | Atualização parcial de um role |
| `DELETE` | `{{base-api-url}}/roles/{{id}}` | Exclusão lógica de um role |

### Atribuição de Roles a Usuários
Além dos endpoints acima, devem ser criados os endpoints para vincular e desvincular papéis a usuários:
- Endpoint para **atribuir um role a um user**
- Endpoint para **remover a atribuição de um role de um user**

---

## 4. Padrão de Tratamento de Erros

Todas as respostas de erro da API devem seguir obrigatoriamente a estrutura JSON abaixo:

### Estrutura do Response Body de Erro
```json
{
  "error_code": "OA-000",
  "error_description": "Descrição amigável do erro ocorrido",
  "error_source": "OAuthAPI",
  "error_stack": [
    {
      "message": "Detalhe técnico ou exceção capturada"
    }
  ]
}
```

### Detalhamento dos Campos
| Campo | Tipo | Descrição |
|---|---|---|
| `error_code` | `string` | Código de erro padronizado. Não havendo uma instrução em sentido contrário, repassar o próprio *response code* do Keycloak. |
| `error_description` | `string` | Descrição clara do erro provida pelo desenvolvedor (grupo). |
| `error_source` | `string` | Origem do erro final (exemplos: `OAuthAPI`, `CoursesAPI`, `BuildingsAPI` etc.). |
| `error_stack` | `array` | Pilha contendo todo o encadeamento de erros até o erro final. |

---

## 5. Material a ser Entregue

- [ ] **Execução via Docker:** API e documentação Swagger funcionais a partir do comando `docker compose up`.
- [ ] **Documentação no `README.md` do GitHub:**
  - [ ] Documentação e esclarecimentos sobre a arquitetura de software adotada.
  - [ ] URL para a documentação interativa do Swagger.
  - [ ] Instruções complementares para execução (caso necessárias).
- [ ] **Versionamento:** Repositório do GitHub devidamente atualizado com o `README.md` e a tag de release criada.
- [ ] **Submissão Moodle:** Envio pelo Moodle de arquivo compactado (`.zip`) contendo exatamente o código-fonte correspondente à tag do GitHub.

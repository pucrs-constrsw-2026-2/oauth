# Especificação da Disciplina: Microserviço OAuth / Keycloak

Fonte: Orientações fornecidas pelo professor.

## 1. Container Keycloak
```bash
docker run -d -p 8080:8080 -v keyclock-data:/opt/jboss/keycloak/standalone/data -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=a12345678 jboss/keycloak
```

## 2. Configurações no Keycloak
- **Realm:** `constrsw` (configurado para utilizar e-mail como username).
- **Client:** `oauth` (com client secret gerado).
- **Roles:** `administrator`, `coordinator`, `professor`, `student`.
- **Resources:** `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students` (com URL definida para cada um).
- **Policies:** tipo "Role", filtradas por client (`administrator-policy`, `coordinator-policy`, `professor-policy`).
- **Permissions:** tipo resource-based:
  - `administrator-permissions`: recursos `resources`, `rooms`, `professors`, `students` vinculados a `administrator-policy`.
  - `coordinator-permissions`: recursos `courses`, `classes` vinculados a `coordinator-policy`.
  - `professor-permissions`: recursos `lessons`, `reservations` vinculados a `professor-policy`.

## 3. Rotas Obrigatórias da API REST (Symfony)
- `POST {{base-api-url}}/login` (recebe client_id, username, password, grant_type: password e retorna tokens).
- `POST {{base-api-url}}/users` (criação de usuário via Keycloak Admin API, retorna 201 Created).
- `GET {{base-api-url}}/users` (recuperação de todos os usuários cadastrados e habilitados, retorna 200 OK).
- `GET {{base-api-url}}/users/{{id}}` (recuperação de um usuário por ID, retorna 200 OK ou 404 Not Found).
- `PUT {{base-api-url}}/users/{{id}}` (atualização de atributos de usuário, retorna 200 OK ou 404 Not Found).
- `PATCH {{base-api-url}}/users/{{id}}` (atualização de senha de usuário, retorna 200 OK ou 404 Not Found).
- `DELETE {{base-api-url}}/users/{{id}}` (exclusão lógica / desabilitação de usuário, retorna 204 No Content ou 404 Not Found).
- Endpoint verificador de autorização: valida access token e verifica se algum dos roles do usuário dá acesso ao recurso (200 OK se autorizado, 403 Forbidden se negado).
- Tratamento de expiração de token: rota de renovação (`refresh_token`).

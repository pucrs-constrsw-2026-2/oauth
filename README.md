# OAuth ConstrSW

Gateway autocontido de identidade institucional do ConstrSW.

## Arquitetura

O módulo `backend/oauth` é um gateway entre o cliente da aplicação e o Keycloak.
O browser fala apenas com este serviço; o serviço, por sua vez, chama o Keycloak
(endpoint OIDC de token para login e a Admin REST API para roles) e devolve ao
consumidor apenas o que ele precisa.

A infraestrutura local é composta por dois containers principais, orquestrados
pelo `docker-compose.yml` da raiz:

- `keycloak`: sobe a partir de uma imagem customizada que importa o realm local
  `constrsw` (`infrastructure/dev.local/services/keycloak/constrsw.json`), expõe
  o console/admin e emite os tokens JWT.
- `oauth`: sobe a API NestJS que implementa login, refresh, logout, healthcheck
  e o CRUD de roles, usando o Keycloak como provedor externo.

Os dois serviços compartilham a rede `constrsw`, e o `oauth` depende de um
Keycloak saudável antes de iniciar. O estado do realm fica persistido no volume
externo `constrsw-keycloak-data`; a importação inicial acontece uma vez e depois
o volume mantém a configuração.

## Tecnologias utilizadas

- Node.js 24 / NestJS 11 / TypeScript
- Keycloak 26 (realm `constrsw`)
- Docker e Docker Compose
- `class-validator` / `class-transformer` para validação de payloads
- `cookie-parser` para o cookie de sessão
- Swagger/OpenAPI para documentação (`/docs`)
- Jest + Supertest para testes unitários e de integração

## Decisões importantes

- **O login pertence a este módulo.** A rota pública é `POST /login`;
  não existe login no BFF. Isso elimina a duplicação de clientes Keycloak e deixa
  autenticação, sessão e configuração num único lugar.
- **O browser nunca chama o Keycloak.** O serviço faz o Direct Access Grant
  internamente, devolve os tokens no corpo de login conforme o contrato e também
  mantém a sessão em cookie `httpOnly`.
- **Operações administrativas usam um token de admin.** As rotas de roles falam
  com a Admin REST API do Keycloak. Para isso o serviço obtém um token via
  `grant_type=password` no cliente `admin-cli` do realm `master`, usando
  `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD`. Esse caminho é separado do login
  de usuário (que usa o cliente confidencial em `KEYCLOAK_CLIENT_ID`).
- **Roles são realm roles do Keycloak.** O serviço não mantém banco próprio: o
  CRUD de roles é um proxy sobre a Admin API. Como realm roles não têm exclusão
  nativa, o `DELETE` é uma **exclusão lógica** (grava o atributo `deleted=true` no
  role); roles marcados somem das leituras.
- **Erros são seguros.** A API responde JSON com apenas `error_code`,
  `error_description`, `error_source` e `error_stack`. O status HTTP permanece
  na resposta HTTP, e nunca devolvemos senha, token, segredo, payload do
  Keycloak ou stack trace.
- **As mutações de usuários usam o Admin API.** O gateway mantém o bearer recebido na superfície pública e usa o cliente administrativo para criar, atualizar, alterar senha e desabilitar usuários.
- **Erros são seguros.** A API responde `application/json` no envelope acordado pelo grupo: `error_code`, `error_description`, `error_source` e `error_stack` (array de `{source, code, description}`).

### Por que um serviço separado?

O OAuth é autocontido porque precisa ser executado, testado e atualizado sem
depender do código de um contexto de domínio. Ele concentra a integração com o
Keycloak (login e administração de identidade), mas não vira dono dos dados de
domínio da aplicação. Os demais módulos consomem uma fronteira estável em vez de
cada equipe implementar seu próprio `fetch` para o provedor.

## Rotas

### Autenticação

| Método | Rota      | Descrição                                        |
| ------ | --------- | ------------------------------------------------ |
| POST   | `/login`  | Autentica um usuário via `multipart/form-data` e devolve os tokens. |
| POST   | `/refresh`| Renova a sessão a partir do cookie.              |
| POST   | `/logout` | Limpa o cookie de sessão.                        |

### Roles (`/roles`)

| Método | Rota                      | Descrição                                    |
| ------ | ------------------------- | -------------------------------------------- |
| POST   | `/`                       | Cria um role. → `201`                        |
| GET    | `/`                       | Lista os roles (exclui os excluídos).        |
| GET    | `/{id}`                   | Recupera um role pelo id. → `404` se ausente |
| PUT    | `/{id}`                   | Atualização total.                           |
| PATCH  | `/{id}`                   | Atualização parcial.                         |
| DELETE | `/{id}`                   | **Exclusão lógica.** → `204`                 |
| POST   | `/{id}/users/{userId}`    | Atribui o role a um usuário. → `204`         |
| DELETE | `/{id}/users/{userId}`    | Remove a atribuição. → `204`                 |

As rotas de roles exigem as credenciais administrativas (`KEYCLOAK_ADMIN*`); sem
elas o serviço não consegue token de admin e responde `503`.

### Usuários (`/users`)

| Método | Rota | Descrição |
| ------ | ---- | --------- |
| POST | `/` | Cria um usuário e retorna o `id` do header `Location`. → `201` |
| GET | `/` | Lista usuários; aceita `?enabled=true|false`. |
| GET | `/{id}` | Recupera um usuário. |
| PUT | `/{id}` | Atualiza os dados do usuário. → `200` |
| PATCH | `/{id}` | Atualiza parcialmente o usuário ou sua senha. → `200` |
| DELETE | `/{id}` | Desabilita logicamente o usuário. → `204` |

## Variáveis de ambiente

| Variável                                  | Exemplo                | Uso                                  |
| ----------------------------------------- | ---------------------- | ------------------------------------ |
| `PORT`                                    | `8088`                 | Porta interna da API                 |
| `KEYCLOAK_URL`                            | `http://keycloak:8080` | Base do Keycloak                     |
| `KEYCLOAK_REALM`                          | `constrsw`             | Realm da aplicação                   |
| `KEYCLOAK_CLIENT_ID` / `_SECRET`          | `oauth` / `…`          | Cliente usado no login               |
| `KEYCLOAK_TIMEOUT_MS`                     | `5000`                 | Timeout das chamadas ao Keycloak     |
| `KEYCLOAK_ADMIN` / `_PASSWORD`            | `admin` / `a12345678`  | Credenciais administrativas          |
| `KEYCLOAK_ADMIN_REALM` / `_CLIENT_ID`     | `master` / `oauth-admin` | Realm e client administrativos      |
| `KEYCLOAK_ADMIN_CLIENT_SECRET`            | `…`                    | Segredo do client administrativo     |
| `SESSION_COOKIE_NAME`                     | `closed_cras_session`  | Nome do cookie de sessão             |
| `COOKIE_SECURE` / `COOKIE_SAME_SITE`      | `false` / `lax`        | Flags do cookie                      |

Em produção, segredo do cliente e credenciais administrativas devem vir de
secret management. Os valores no Compose existem apenas para desenvolvimento.

## Executar

### Via Docker (recomendado)

A partir da **raiz do repositório** — sobe Keycloak (realm `constrsw`) e a API
com a configuração consistente do `docker-compose.yml`:

```bash
docker volume create constrsw-keycloak-data   # apenas na primeira vez
docker compose up -d --build --wait
```

Endereços: API `http://localhost:8181`, saúde `GET /health`, Swagger
`http://localhost:8181/docs`, Keycloak `http://localhost:8081`.

> Após mudar `.env` ou o compose, recrie o container:
> `docker compose up -d --build --force-recreate oauth`.

### Fora do container (opcional)

Requer Node.js 24. Instale com `--legacy-peer-deps` (há um conflito de peers
`typescript`/`ts-jest` conhecido nesta base):

```bash
cp .env.example .env
npm install --legacy-peer-deps
npm run start:dev
```

O `.env.example` já traz `KEYCLOAK_ADMIN*`, mas os valores de conexão apontam
para nomes internos de container. Para rodar localmente **contra o Keycloak do
Compose**, ajuste no seu `.env`:

```dotenv
KEYCLOAK_URL=http://localhost:8081
KEYCLOAK_REALM=constrsw
KEYCLOAK_CLIENT_ID=oauth
KEYCLOAK_CLIENT_SECRET=wsNXUxaupU9X6jCncsn3rOEy6PDt7oJO
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=a12345678
```

## Rodar os testes
Quando usar `-d`, os containers ficam em segundo plano e os logs não aparecem no
terminal. Para acompanhar o startup e visualizar as URLs impressas pelo OAuth:

```bash
docker compose logs -f oauth
```

As URLs públicas são API `http://localhost:8181`, saúde
`http://localhost:8181/health`, Swagger `http://localhost:8181/docs` e Keycloak
`http://localhost:8081`.

Endereços locais: API `http://localhost:8181`, saúde `GET /health`, Swagger `http://localhost:8181/docs` e Keycloak `http://localhost:8081`.

```bash
npm run test        # unitários (Jest)
npm run test:e2e    # integração: sobe a app Nest e simula o Keycloak (não precisa de Keycloak vivo)
npm run build       # compila/verifica tipos
```

## Teste rápido

Com o stack no ar (as rotas de roles não exigem login):

```bash
BASE=http://localhost:8181

# Criar um role → 201 com { id, name, description }
curl -i -X POST $BASE/roles -H 'content-type: application/json' \
  -d '{"name":"professor","description":"Docente"}'

# Listar / buscar / excluir logicamente
curl -s  $BASE/roles | jq
curl -i -X DELETE $BASE/roles/<ID>   # 204
curl -i $BASE/roles/<ID>             # 404 (some após a exclusão lógica)
```

O realm `constrsw` já vem com usuários (`admin@pucrs.br`, `coordinator@pucrs.br`,
`professor@pucrs.br`, `student@pucrs.br`); as senhas são gerenciadas no Keycloak
(console em `http://localhost:8081`). Para testar o login, use um desses usuários:

```bash
curl -i -X POST $BASE/login -F 'username=professor@pucrs.br' \
  -F 'password=<senha-no-keycloak>'
```

O corpo de sucesso contém `token_type`, `access_token`, `expires_in`,
`refresh_token` e `refresh_expires_in`; a sessão também é armazenada no cookie
`httpOnly`.

## Contratos

- `contracts/identity-gateway.yaml`: fragmento OpenAPI das rotas de autenticação,
  usuários e roles.
- `infrastructure/dev.local/services/keycloak/constrsw.json` (na raiz do repo):
  realm importado pelo container do Keycloak.
- `keycloak/realm-closed-cras.json`: realm legado de exemplo; **não** é o que sobe
  em desenvolvimento (o realm ativo é `constrsw`).

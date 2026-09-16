# OAuth ConstrSW

Gateway autocontido de identidade institucional para o laboratório Closed CRAS.

## Arquitetura

O módulo `backend/oauth` é um gateway de autenticação entre o cliente da aplicação e o Keycloak. O browser fala apenas com este serviço; o serviço, por sua vez, chama o endpoint OIDC de token do Keycloak usando Direct Access Grant e devolve somente metadados da sessão para o consumidor.

A infraestrutura local é composta por dois containers principais, orquestrados pelo `docker-compose.yml` da raiz:

- `keycloak`: sobe a partir de uma imagem customizada que empacota o realm local `constrsw.json`, expõe o console/admin e fornece os tokens JWT.
- `oauth`: sobe a API NestJS que implementa login, refresh, logout e healthcheck, usando o Keycloak como provedor externo.

Os dois serviços compartilham a rede `constrsw`, e o OAuth depende de um Keycloak saudável antes de iniciar. O estado do realm fica persistido no volume externo `constrsw-keycloak-data`, então a importação inicial acontece uma vez e depois o volume mantém a configuração.

## Tecnologias utilizadas

- Node.js 24.19.0
- NestJS 11
- TypeScript 6
- Keycloak 26
- Docker e Docker Compose
- `class-validator` e `class-transformer` para validação dos payloads
- `cookie-parser` para leitura do cookie de sessão
- Swagger/OpenAPI para documentação da API

## Decisões importantes

- **O login pertence a este módulo.** O `backend/oauth` é um serviço executável e autocontido. A rota pública é `POST /v1/auth/login`; não existe login no BFF. **Justificativa:** a decisão atual elimina a duplicação de clientes Keycloak e deixa autenticação, sessão e configuração em um único lugar. Isso torna o módulo fácil de subir isoladamente e reduz o risco de cada contexto criar uma interpretação diferente do login.
- **O browser nunca chama o Keycloak.** O serviço faz o Direct Access Grant internamente e guarda access token e refresh token em cookie `httpOnly`. Assim, tokens não ficam expostos ao JavaScript nem ao `localStorage`. **Justificativa:** o browser conhece apenas a API da aplicação, o que reduz a superfície de exposição do IdP e evita espalhar URL, client secret ou regras de Keycloak pelo frontend. O trade-off é que a API precisa cuidar de refresh, logout e configuração de cookie.
- **`client_credentials` não autentica pessoas.** Esse fluxo fica reservado para futuras chamadas internas administrativas, quando o ownership for confirmado. O login de usuário usa `grant_type=password` no cliente confidencial configurado em `KEYCLOAK_CLIENT_ID`. **Justificativa:** `client_credentials` identifica uma aplicação, não uma pessoa, portanto não pode representar o usuário institucional nem produzir uma sessão pessoal. Separar os fluxos também evita que uma credencial administrativa seja usada acidentalmente no caminho de login.
- **Erros são seguros.** A API responde `application/problem+json` com `type`, `title`, `status`, `code` e `detail`. Nunca devolvemos senha, token, segredo, payload do Keycloak ou stack trace. **Justificativa:** mensagens do provedor podem conter detalhes úteis para um atacante, além de serem instáveis para consumidores. Um contrato pequeno e estável permite que frontend e demais contextos tratem `401`, `400` e `503` sem depender da implementação interna.
- **O escopo é identidade.** CRUD de usuários, roles e role-mapping não faz parte desta base; eles serão adicionados somente com contrato próprio. **Justificativa:** login e infraestrutura transversal são pré-requisitos para as outras trilhas, enquanto CRUD exige decisões próprias de autorização, validação e ciclo de vida. Mantê-los fora evita transformar este primeiro serviço em um catálogo administrativo sem contrato fechado.

### Por que um serviço separado?

O OAuth é autocontido porque precisa ser executado, testado e atualizado sem depender do código de um contexto de domínio. O serviço concentra a integração com o Keycloak, mas não se torna dono dos dados de usuários ou roles da aplicação. Essa separação permite que os demais módulos consumam uma fronteira estável e evita que cada equipe implemente seu próprio `fetch` para o provedor.

### Fluxo de implementação

1. O cliente chama `POST /v1/auth/login` com `username` e `password`.
2. O Nest valida o payload e envia `grant_type=password` para o Keycloak usando `KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` e `KEYCLOAK_CLIENT_SECRET`.
3. Se o token for emitido, o serviço grava `access_token` e `refresh_token` em cookie `httpOnly` e retorna apenas `token_type`, `expires_in` e `refresh_expires_in`.
4. O cliente renova a sessão em `POST /v1/auth/refresh`, que lê o cookie, usa `grant_type=refresh_token` e regrava a sessão.
5. O logout em `POST /v1/auth/logout` apenas limpa o cookie local.

As falhas do provedor são normalizadas em `application/problem+json`, sem vazar payload bruto, segredo ou stack trace.

Em produção, o segredo do cliente, as credenciais administrativas e o usuário demo devem vir de secret management. Os valores presentes no Compose existem apenas para desenvolvimento local e não devem ser reutilizados fora dele.

## Executar

Requisitos: Node.js 24.19.0, npm e Docker Desktop.

```powershell
Copy-Item .env.example .env
npm install
npm run start:dev
```

A partir da raiz do repositório, para iniciar Keycloak e a API juntos:

```powershell
docker compose up --build
```

Endereços locais: API `http://localhost:8181`, saúde `GET /health`, Swagger `http://localhost:8181/docs` e Keycloak `http://localhost:8081`.

Usuário de demonstração: `demo@pucrs.br` / `demo`. Troque os segredos antes de qualquer ambiente compartilhado.

## Teste rápido

```powershell
Invoke-RestMethod -Method Post http://localhost:8181/v1/auth/login `
	-ContentType 'application/json' `
	-Body '{"username":"demo@pucrs.br","password":"demo"}'
```

O corpo de sucesso contém apenas metadados da sessão. O cookie contém os tokens e é `httpOnly`.

## Contratos

- `contracts/identity-gateway.yaml`: fragmento OpenAPI compartilhável.
- `keycloak/realm-closed-cras.json`: realm local importável.
- `Planning/Rotas.md`: documento legado do enunciado; não é fonte executável e contém decisões superadas.

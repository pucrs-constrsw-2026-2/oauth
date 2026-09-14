# Contexto do projeto — API OAuth (grupo08 / ConstrSW 2026-2)

> Este documento existe para dar contexto rápido a qualquer IA (Claude Code, etc.) ou pessoa do grupo que for continuar o trabalho. Reflete o estado das decisões até o momento — algumas ainda dependem de confirmação externa (marcado abaixo).

## O que é o projeto

API REST em Java + Spring Boot que atua como *adapter* para a REST API do Keycloak: login, refresh token, CRUD de usuários e roles. Parte de um sistema maior (ConstrSW) com vários microsserviços (employees, rooms, courses, bff, frontend, etc.), cada um implementado por um grupo diferente da turma, todos no mesmo repositório `oauth` (org `pucrs-constrsw-2026-2` no GitHub), branch `grupo08`.

## Stack e decisões de arquitetura (fechadas)

| Decisão | Escolha | Motivo |
|---|---|---|
| Linguagem | Java + Spring Boot | Alinhado ao exemplo de referência do professor |
| Build/execução | Só dentro de Docker | Ninguém instala Java/Maven local — projeto roda em nuvem no final |
| Validação de token | Local via JWKS (`spring-boot-starter-oauth2-resource-server`) | Padrão de mercado, sem round-trip a cada request |
| `client_id`/`client_secret` | Fixos via variável de ambiente | Grant `password` pressupõe client confidencial |
| Rota de refresh | `POST /refresh-token` separada do `/login` | Trata expiração de token de forma explícita |
| Contrato de erro | `{error_code, error_description, error_source, error_stack}` centralizado via `@RestControllerAdvice` | Exigido pelo enunciado, evita duplicação |

## Infraestrutura Docker — MUDANÇA IMPORTANTE (aula de 02/09)

O professor anunciou que vai fornecer um **`docker-compose.yml` único e compartilhado** para toda a turma, que builda o `Dockerfile` de cada grupo. Isso muda o que era nosso plano anterior (compose próprio). Pontos confirmados na aula:

- Compose fica na raiz do repositório principal, sobe **toda** a infraestrutura de uma vez.
- Nenhum valor literal no compose — tudo vem de `.env`.
- Nome do container = hostname interno na rede Docker (ex: `http://oauth` para chamadas de outros serviços).
- Porta externa é só para acesso do host; comunicação entre containers usa porta interna.
- Health check via `curl -f <url>/health` — 200 = healthy. Serviços dependentes só sobem com `condition: service_healthy`.
- Volumes externos (criados manualmente com `docker volume create <nome>`) para serviços com persistência (Postgres, Mongo, GitLab, Prometheus — Keycloak possivelmente também).
- `restart: always` só em infraestrutura de terceiros, não em serviços dos grupos.
- **Validação adiada para segunda-feira, 07/09.**
- Cada grupo só precisa ajustar variáveis no `.env`, não mexer no compose em si.

### O que isso muda pra gente

- ❌ **Removido**: nosso `docker-compose.yml` próprio (estava na raiz do repo `oauth`) — não será mais usado.
- ❓ **A confirmar**: se `keycloak/realm-export.json` continua sendo nossa responsabilidade, ou se o Keycloak (com realm/client já configurados) também vira infraestrutura fornecida centralmente. **Hipótese forte: vai ser fornecido, não mais por nós.**
- ✅ **Mantido sem alteração**: `Dockerfile` do projeto (é a única coisa que o compose externo vai consumir de nós).
- ✅ **Mantido sem alteração**: todo o código Java (`SecurityConfig`, `KeycloakProperties`, `WebClientConfig`, exceptions) — já lê tudo via variável de ambiente, só muda o valor recebido, não a lógica.
- 🔧 **Ajustar**: `.env.example` — remover variáveis que passam a ser gerenciadas centralmente (ex: admin do Keycloak), manter só as específicas do nosso serviço (`KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`). `KEYCLOAK_BASE_URL` provavelmente vem fixo do compose central (nome do container dele), não do nosso `.env`.
- ✅ **Confirmado**: não precisa clonar o repositório `base` nem criar branch lá — o compose compartilhado é responsabilidade do professor, não nossa. Todo o trabalho continua dentro do repo `oauth`, branch `grupo08`.

## Sincronização e histórico recente

- A entrega da branch `grupo08-feat/lucas` foi integrada à `grupo08` pelo PR #14.
- O merge inclui Auth, CRUD de usuários, CRUD de roles, associação/desassociação de roles e a coleção Bruno correspondente.
- O commit `698ea20` encerra a entrega da Pessoa 4; o ajuste posterior `2842bb7` altera a `baseUrl` de `8081` para `8181`.

## Divisão do trabalho (grupo, 4 pessoas)

- **Arthur**: arquitetura, infra, config central (`KeycloakProperties`, `WebClientConfig`, `SecurityConfig`), contrato de erro compartilhado — **testado e funcional** até a mudança de infraestrutura acima.
- **Pessoa 3 (Lucas)**: Auth (`/login`, `/refresh-token`) — **implementado e integrado**.
- **Pessoa 4 (Anthony)**: Users e Roles (CRUD completo e associação de roles) — **implementado, testado e integrado**.
- **João Biasoli**: Astah (User/Role já concluído), Postman, README, apresentação.
- Regra geral: toda parte precisa de revisão de outra pessoa antes do merge, sem exceção fixa de quem revisa quem.

## Estado atual do código (branch `grupo08`, repo `oauth`)

- App Spring Boot e Keycloak sobem via Docker.
- Swagger público em `/docs`.
- Login e refresh token implementados.
- Usuários: criação, listagem, consulta, atualização, troca de senha e desativação.
- Roles de realm: criação, listagem, consulta, atualização parcial/completa e remoção.
- Associação e desassociação de roles de realm a usuários.
- Rotas administrativas protegidas por bearer token.
- Contratos de requisição/resposta e tratamento centralizado de erros.
- Coleção Bruno cobrindo Auth, Users e Roles.
- Suíte automatizada validada com **80 testes, sem falhas ou erros**.
- Smoke test integrado validou login, Users, Roles e associação/desassociação de role.

### Observações conhecidas

- O health check do compose compartilhado pode marcar o container como `unhealthy` por invocar `node`, ausente na imagem Java, embora `/health` responda HTTP 200.
- Uma chamada sem autenticação a `/users` retorna HTTP 401 sem o corpo padronizado de erro porque é interceptada pelo filtro do Spring Security antes do `@RestControllerAdvice`.

## Pendências / próximos passos

1. Corrigir o health check do compose compartilhado para usar uma ferramenta presente na imagem Java (por exemplo, `curl`).
2. Padronizar o corpo da resposta 401 gerada pelo Spring Security, se exigido pelo contrato da disciplina.
3. Confirmar se `keycloak/realm-export.json` continua sob responsabilidade do grupo.
4. Revisar `.env.example` conforme as variáveis definitivas do compose compartilhado.
5. João finalizar os artefatos restantes de documentação e apresentação.

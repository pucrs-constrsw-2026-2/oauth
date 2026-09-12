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

## Sincronização pendente

A `main` do `oauth` foi alterada pelo professor/curso depois que criamos a `grupo08`. Precisa:
```bash
git fetch origin
git checkout grupo08
git merge origin/main
```
(resolver conflitos triviais; qualquer conflito não-trivial deve ser revisado manualmente, não resolvido automaticamente por uma IA sem revisão)

## Divisão do trabalho (grupo, 4 pessoas)

- **Arthur**: arquitetura, infra, config central (`KeycloakProperties`, `WebClientConfig`, `SecurityConfig`), contrato de erro compartilhado — **testado e funcional** até a mudança de infraestrutura acima.
- **Pessoa 3**: Auth (`/login`, `/refresh-token`) — ainda não implementado.
- **Pessoa 4**: Users e Roles (CRUD completo) — ainda não implementado.
- **João Biasoli**: Astah (User/Role já concluído), Postman, README, apresentação.
- Regra geral: toda parte precisa de revisão de outra pessoa antes do merge, sem exceção fixa de quem revisa quem.

## Estado atual do código (branch `grupo08`, repo `oauth`)

Testado rodando de ponta a ponta via `docker compose up` (com nosso compose antigo, antes da mudança de infra):
- App Spring Boot sobe sem erro.
- Keycloak local importava o realm `constrsw` corretamente.
- Swagger público em `/docs` (após corrigir rotas do springdoc faltando no `SecurityConfig`).
- Rotas protegidas retornando 401 sem token (comportamento esperado, ainda sem controllers de negócio).
- `.gitignore` protegendo `.env`.

## Pendências / próximos passos

1. Fazer merge da `main` atualizada na `grupo08`.
2. Remover `docker-compose.yml` próprio do repo `oauth`.
3. Decidir (aguardando confirmação) se `keycloak/realm-export.json` sai também.
4. Ajustar `.env.example` para as variáveis que continuam sendo nossas.
5. Aguardar o compose oficial do professor (validação: segunda, 07/09) e integrar — provavelmente só ajuste de nomes de variável.
6. Pessoa 3 implementar `AuthController` + `KeycloakAuthService`.
7. Pessoa 4 implementar `UserController`/`RoleController` + services correspondentes.
8. João finalizar Postman collection + README com as decisões documentadas acima.

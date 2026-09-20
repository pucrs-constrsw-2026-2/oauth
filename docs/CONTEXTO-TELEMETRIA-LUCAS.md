# Contexto Técnico — Telemetria, Erros Padronizados e Revisão de Segurança (Grupo 08)

> **Data:** 19-20/09/2026
> **Branch de Trabalho:** `grupo08-feat/telemetry-lucas` (mergeada em `grupo08` via PR [#23](https://github.com/pucrs-constrsw-2026-2/oauth/pull/23), merge commit `08faac7`)
> **Base de Comparação:** `origin/grupo08`
> **Objetivo:** Registrar o que foi entregue na feature de telemetria/observabilidade, os achados do code review (automático e manual) que surgiram no processo e como cada um foi resolvido antes do merge.

---

## 1. Visão Geral da Entrega

A branch `grupo08-feat/telemetry-lucas` adiciona a camada de **observabilidade** ao microsserviço OAuth (OpenTelemetry + Prometheus), separa o tráfego de métricas/management do tráfego de negócio, e padroniza as respostas 401/403 diretamente na cadeia de filtros do Spring Security — complementando o contrato de erro já existente (`OA-4xx`/`OA-5xx`) de [`CONTEXTO-DESENVOLVIMENTO-LUCAS.md`](./CONTEXTO-DESENVOLVIMENTO-LUCAS.md).

O PR passou por **duas rodadas de revisão** antes do merge: a automática do Copilot code review e uma auditoria manual de segurança/qualidade (skill `vibecode-security-review`), ambas com achados reais corrigidos — nenhuma delas foi só formalidade.

---

## 2. Histórico de Commits desta Entrega

| Hash | Mensagem | O que foi feito |
|---|---|---|
| `8be121e` | `feat(telemetry): integrar opentelemetry e prometheus, padronizar erros 401/403 e documentar arquitetura completa` | Dependências `micrometer-registry-prometheus`/`micrometer-tracing-bridge-otel`, porta de métricas isolada (9464/8381), `HealthController` dedicado, handlers 401/403 no `SecurityConfig`, `prometheus.yml` + override do compose, suíte Bruno/Postman atualizada, README expandido como documento formal de entrega. |
| `3aed0b0` | `fix: corrigir achados do code review do Copilot no PR #23` | Ver seção 3 — os 8 achados do Copilot, todos verificados no código antes de corrigir. |
| `c46a4cc` | `fix(security): política de senha mínima e proteção contra força bruta no login` | Ver seção 4 — 3 achados médios da auditoria manual de segurança. |
| `08faac7` | `Merge pull request #23` | Merge (merge commit, sem squash/rebase) em `grupo08`. |

---

## 3. Achados do Copilot Code Review (commit `3aed0b0`)

O Copilot abriu 8 comentários no PR (1 alto, 4 médios, 3 baixos). Cada um foi **verificado lendo o código real** antes de aceitar — nenhum foi corrigido só por confiança no relatório automático.

| Severidade | Achado | Onde | Correção |
|---|---|---|---|
| 🔴 Alto | Tokens gravados em `pm.collectionVariables` (não-secreta) mesmo o ambiente marcando `accessToken`/`refreshToken` como `secret` | `postman/oauth.postman_collection.json` | Trocado para `pm.environment.set(...)` nos 3 fluxos (login urlencoded, login multipart, refresh) |
| 🟡 Médio | `bruno/environments/Local.bru` usava `admin@pucrs.br`, o README documentava `student@pucrs.br` — nenhum dos dois existe no realm exportado, que só provisiona `testuser`/`test123` | `bruno/environments/Local.bru`, `bruno/README.md` | Alinhados ao único usuário real do `realm-export.json` |
| 🟡 Médio | Job Prometheus do Keycloak apontava para `keycloak:9001`, porta que o `docker-compose.yml` nunca publica nem habilita métricas — o target ficaria `DOWN` para sempre | `prometheus/prometheus.yml`, `docker-compose.yml` | Habilitado `KC_METRICS_ENABLED`/`KC_HEALTH_ENABLED` no Keycloak 26.x, exposta a porta real de management (9000) e corrigido o target |
| 🟡 Médio | Teste de `SecurityConfig` chamava `authenticationEntryPoint()`/`accessDeniedHandler()` direto, sem passar pela `SecurityFilterChain` real — uma regressão na ligação dos handlers ao filtro passaria despercebida | `src/main/java/.../config/SecurityConfig.java` | Novo teste `SecurityFilterChainIntegrationTest` (`@WebMvcTest` + `MockMvc`) — ver seção 3.1, achado bônus |
| 🟡 Médio | `show-details: always` no `/actuator/health`, exposto sem autenticação (`/actuator/**` é `permitAll`) | `src/main/resources/application.yml` | Revertido para `show-details: never` |
| 🟢 Baixo | Links `file:///Users/lns7_/...` e caminhos absolutos de execução hardcoded na máquina do autor original | `README.md` (linhas 122, 165, 342, 372, 384) | Trocados por caminhos relativos ao repositório / placeholder |
| 🟢 Baixo | Query de exemplo `http_server_requests_seconds{quantile="0.95"}` não retorna série nenhuma, pois o app configura `percentiles-histogram` (buckets), não `percentiles` (gauge) | `README.md` (linha 224) | Trocada por `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` |

### 3.1 Achado bônus descoberto ao implementar o teste pedido pelo Copilot

Ao escrever o `SecurityFilterChainIntegrationTest` para cobrir a integração real (não só os handlers isolados), o teste de 403 **falhava** com `OA-500` em vez de `OA-403`. Causa raiz: `GlobalExceptionHandler` tinha um `@ExceptionHandler(Exception.class)` genérico que capturava `AccessDeniedException` antes dela chegar ao `ExceptionTranslationFilter` do Spring Security — ou seja, se algum dia uma checagem de permissão (`@PreAuthorize`, policy customizada) lançasse essa exceção, o cliente receberia um 500 genérico em vez do contrato `OA-403` padronizado.

**Correção:** adicionado um `@ExceptionHandler(AccessDeniedException.class)` explícito em `GlobalExceptionHandler` que relança a exceção, deixando o Spring Security tratá-la. Exatamente o tipo de regressão que o comentário original do Copilot alertava ser possível — e que só o teste de integração pegou.

---

## 4. Auditoria de Segurança Manual (commit `c46a4cc`)

Depois de resolvidos os achados do Copilot, foi rodada uma auditoria adicional usando a skill `vibecode-security-review` (checklist de segredos, auth/autorização, injeção, configuração/exposição, dados sensíveis em log, dependências) sobre o repositório inteiro — não só o diff do PR.

**Nenhum achado Crítico ou Alto novo.** O design de autorização foi verificado ponta a ponta: `/users` e `/roles` delegam a checagem de permissão para a Admin API do Keycloak (repassando o Bearer do usuário chamador), sem `@PreAuthorize` local — e isso é seguro, porque o Keycloak rejeita quem não tem `realm-management`/`manage-users` no token.

**3 achados médios corrigidos:**

| Achado | Onde | Correção |
|---|---|---|
| Sem política de senha mínima — API e realm aceitavam qualquer senha não vazia (`@NotBlank` só) | `CreateUserRequest.java`, `UpdatePasswordRequest.java`, `keycloak/realm-export.json` | `@Size(min = 8)` nos DTOs + `passwordPolicy: "length(8)"` no realm |
| `POST /login` sem nenhuma proteção contra força bruta de senha | `keycloak/realm-export.json` | `bruteForceProtected: true` com lockout progressivo padrão do Keycloak (`maxFailureWaitSeconds`, `waitIncrementSeconds`, `failureFactor: 5`, etc.) |
| `error_stack` do handler de 500 devolve `ex.toString()` (detalhe interno da exceção) ao cliente | `GlobalExceptionHandler.java` (`handleGeneric`) | **Não alterado** — ver nota abaixo |

**Nota sobre o `error_stack` do 500:** este campo é parte do contrato de erro documentado como exigido pelo enunciado da disciplina (`ErrorResponse.java`) e é usado de forma consistente em *todos* os handlers do `GlobalExceptionHandler`, não só no genérico. Mudar esse comportamento só no handler de 500 quebraria a consistência do contrato e pode não ser o que a avaliação espera — por isso ficou registrado aqui como decisão consciente de não mexer, em vez de uma correção silenciosa. Se a intenção for endurecer isso para produção fora do contexto acadêmico, o ajuste é trivial (trocar `ex.toString()` por uma mensagem genérica nesse handler específico).

---

## 5. Como Executar e Validar

Suíte de testes unitários/integração (JUnit 5 + Mockito + MockMvc):
```bash
cd backend/oauth
mvn clean test
```
*Resultado comprovado nesta entrega: **87 testes, 0 falhas** (85 pré-existentes + 2 novos: `SecurityFilterChainIntegrationTest` cobrindo 401/403 via `MockMvc` contra a `SecurityFilterChain` real).*

Métricas e telemetria:
```bash
docker compose \
  -f docker-compose.yml \
  -f backend/oauth/docker-compose.base.override.yml \
  up -d --build keycloak oauth prometheus
```
- Métricas do `oauth`: `http://localhost:9464/actuator/prometheus`
- Métricas do Keycloak: `http://localhost:9000/metrics`
- Prometheus: `http://localhost:9090`

---

## 6. Rastreabilidade da Revisão

- PR: [#23](https://github.com/pucrs-constrsw-2026-2/oauth/pull/23) — `grupo08-feat/telemetry-lucas` → `grupo08`
- Review automático: Copilot code review (8 comentários, todos confirmados e respondidos inline)
- Aprovação: registrada no PR após as correções do commit `3aed0b0`
- Merge: merge commit `08faac7` (sem squash/rebase, preservando o histórico)

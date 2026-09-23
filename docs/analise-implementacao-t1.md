# Análise de implementação — T1 `oauth` (grupo03)

**Data da análise:** 2026-09-14 (atualizado 2026-09-23 com observabilidade)
**Branch analisada:** `grupo03` (repo `oauth`, submódulo de `base`)
**Objetivo:** conferir a implementação atual do microsserviço `oauth` contra o enunciado do T1, ponto a ponto, e apontar o que falta antes da entrega. Documento para o grupo, não substitui o `SPEC.md` (que é a referência técnica de contrato).

## Resumo executivo

O código do `oauth` está **completo e correto** em relação a todas as rotas exigidas (login, CRUD de users, CRUD de roles, atribuição de role) e ao formato de erro. Testes unitários e de integração cobrem os fluxos principais. O que falta é **fora do código do `oauth`**:

1. **Bloqueador de ambiente (resolvido):** a branch do grupo no repo `base` (`oauthgrupo3`) estava desatualizada — sem `docker-compose.yml`, `.env` nem `infrastructure/` (backup do Keycloak, Prometheus, OTel Collector). Já mergeado localmente com `origin/main` (duas vezes: infra base + Prometheus/OTel). Falta só dar push.
2. **Pendente de entrega:** ainda não existe tag no repo `oauth` (exigida no enunciado).
3. Dois pontos menores de aderência ao texto do enunciado (cosméticos, já documentados como decisão consciente no `SPEC.md`).

## 1. Bloqueador: branch `oauthgrupo3` do repo `base` desatualizada (resolvido localmente)

O enunciado pressupõe que, ao fazer checkout da branch do grupo **no repo `base`**, o `docker-compose.yml` "que está lá" já sobe o Keycloak. Isso não era verdade pra `oauthgrupo3` até o merge feito nesta sessão:

```
git merge origin/main   # trouxe docker-compose.yml, .env, infrastructure/ (Keycloak)
git merge origin/main   # segunda vez, trouxe Prometheus + OTel Collector (commit e201543)
```

Ambos os merges foram limpos, sem conflito. `oauthgrupo3` local está 13 commits à frente de `origin/oauthgrupo3` — falta só `git push`.

## 2. Cobertura de rotas — `USERS`

| Rota | Implementada | Observação |
|---|---|---|
| `POST /login` | ✅ | `multipart/form-data`, grant `password`, 201/400/401. Remapeia 400→401 do Keycloak (RFC 6749 vs enunciado) — decisão documentada no `SPEC.md` §2. |
| `POST /users` | ✅ | 201/400/401/403/409. Validação de e-mail via regex próprio (ver §4). |
| `GET /users` (+ `?enabled=`) | ✅ | Filtro por query string implementado (`ListUsersQueryDto`). |
| `GET /users/:id` | ✅ | 200/400/401/403/404. |
| `PUT /users/:id` | ✅ | Atualização parcial (só aplica campos enviados) — mais permissivo que "PUT completo" clássico, mas cobre o requisito. |
| `PATCH /users/:id` | ✅ | Só senha, via `reset-password` no Keycloak. |
| `DELETE /users/:id` | ✅ | Exclusão lógica (`enabled: false`), 204. |

## 3. Cobertura de rotas — `ROLES`

| Rota | Implementada | Observação |
|---|---|---|
| `POST /roles` | ✅ | 201/400/401/403/409 (nome duplicado). |
| `GET /roles`, `GET /roles/:id` | ✅ | |
| `PUT /roles/:id` | ✅ | Completo, `name` obrigatório. |
| `PATCH /roles/:id` | ✅ | Parcial. |
| `DELETE /roles/:id` | ✅ | **Exclusão real**, não lógica — Keycloak não tem flag `enabled` pra role. Decisão documentada. |
| `POST /users/:userId/roles/:roleId` | ✅ | Atribuição, 204/401/403/404. |
| `DELETE /users/:userId/roles/:roleId` | ✅ | Remoção da atribuição. |

## 4. Formato de erro

Bate exatamente com o enunciado: `error_code`, `error_description`, `error_source`, `error_stack[]` (`ErrorResponseDto` / `OAuthExceptionFilter`, global). `error_code` = código HTTP da resposta por padrão, sobrescrito só onde o enunciado exige (login 400→401). `error_source` sempre `"OAuthAPI"` na saída, com a causa raiz (`"Keycloak"`) dentro de `error_stack`.

**Duas divergências textuais do enunciado, ambas documentadas como decisão consciente no `SPEC.md`/`README.md`:**

- **Regex de e-mail**: o enunciado colou um regex "RFC 5322" que veio corrompido (chaves/colchetes desbalanceados — provável dano de copiar de PDF/Doc). O grupo usa uma variante RFC 5322-like funcional (`email-rfc5322.validator.ts`). Recomendo manter, mas vale citar isso explicitamente na entrega (Readme ou nota na correção) pra não parecer desvio não-intencional.
- **`refresh_expires_in`**: o enunciado pede `referesh_expires_in` (com erro de digitação). O grupo devolve o campo com o nome correto (`refresh_expires_in`), exatamente como o Keycloak retorna. Correto tecnicamente; só registrar que é intencional caso o corretor use o nome literal do enunciado num teste automatizado.

## 5. Infra e entregáveis do enunciado

| Item exigido | Status |
|---|---|
| `Dockerfile` no repo `oauth` | ✅ (`Dockerfile`, multi-stage, node:24-alpine) |
| API sobe via `docker compose up` | ✅ validado nesta sessão de ponta a ponta (keycloak + oauth healthy) |
| Swagger funcional | ✅ `/swagger` (`main.ts`), com `ApiBearerAuth`, `ApiBody`, `ApiResponse` em todas as rotas |
| `README.md` com arquitetura + URL do Swagger + instruções | ✅ presente e completo |
| Testes unitários | ✅ `src/**/*.spec.ts` — 32/32 passando |
| Testes de integração | ✅ `test/*.e2e-spec.ts` — 30/30 passando (`maxWorkers: 1` no `jest-e2e.json` pra evitar corrida contra o Keycloak real) |
| **Tag no GitHub** | ❌ nenhuma tag encontrada (`git tag -l` vazio) — pendente antes da entrega |
| Envio pelo Moodle do zip da tag | ❌ depende do item acima |

## 6. Observabilidade (Prometheus + OTel) — adicionado 2026-09-23

Commit `e201543` no `main` do repo `base` habilitou Prometheus + OTel Collector centralmente. Pro `oauth`:

- `src/common/telemetry/tracing.ts`: bootstrap do OpenTelemetry Node SDK (primeiro import de `main.ts`), com `HttpInstrumentation`/`ExpressInstrumentation` (métricas automáticas de toda rota) + `PrometheusExporter` expondo `GET /metrics` na porta `OAUTH_INTERNAL_METRICS_PORT` (9464).
- Corrigido `infrastructure/dev.local/services/prometheus/prometheus.yml`: o job `auth` apontava pro hostname `auth:9464`, mas nosso serviço no `docker-compose.yml` se chama `oauth` — corrigido o target pra `oauth:9464` (job `health-checks` também tinha a mesma inconsistência, corrigida).
- Validado via `docker compose up -d --build`: target `auth` (→ `oauth:9464/metrics`) e health-check `oauth:3001/health` aparecem **up** em `http://localhost:9090/api/v1/targets`.
- Ver detalhes de design em `SPEC.md` §6.1.

## 7. Recomendação de próximos passos pro grupo

1. Dar push em `oauthgrupo3` (repo `base`, 13 commits locais) e em `grupo03` (repo `oauth`, telemetria + docs) depois de revisar o diff.
2. Criar a tag de entrega no repo `oauth` e conferir que o zip gerado a partir dela contém exatamente o código esperado.
3. Nada mais a fazer no código do `oauth` pros requisitos funcionais do T1 — está pronto.

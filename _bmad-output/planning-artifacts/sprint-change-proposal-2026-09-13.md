---
title: Sprint Change Proposal — Observabilidade via Grafana
created: 2026-09-13
status: approved
scope: minor
---

# Sprint Change Proposal: Observabilidade da Plataforma via Grafana

## 1. Issue Summary

Requisito novo, trazido pela disciplina/professor, surgido após a conclusão dos 4 épicos originais do oauth (todos `done`): a plataforma precisa de telemetria/observabilidade, começando por um serviço Grafana. A coleta de métricas (Prometheus) é uma frente de trabalho separada, conduzida por outro integrante do time, em paralelo.

Nenhum documento existente (PRD, arquitetura ou Non-Goals do oauth) previa observabilidade — não havia conflito de escopo, apenas uma lacuna a preencher.

## 2. Impact Analysis

- **Epic Impact:** Nenhum epic existente (1–4) foi alterado; todos seguem `done`. Adicionado um novo **Epic 5** dedicado.
- **Story Impact:** Nova Story 5.1 (provisionamento do Grafana). Story de Prometheus não foi criada aqui — pertence à frente do colega.
- **Artifact Conflicts:**
  - PRD: nova seção 4.4 / NFR-7 (Observabilidade via Grafana).
  - Arquitetura: nova decisão AD-7 (Grafana no compose raiz da malha, datasource Prometheus pré-provisionado apontando para host `prometheus`) + linha na Stack Tecnológica.
  - Nota: a arquitetura do **projeto raiz** (fora do oauth) tinha uma decisão anterior (AD-7 da malha) de não incluir Prometheus/stack de observabilidade. Por decisão explícita do usuário, essa divergência não foi documentada nem reconciliada — o Grafana foi adicionado ao compose raiz mesmo assim.
- **Technical Impact:** Alterações de infraestrutura no repositório `base` (fora do submodule oauth): `docker-compose.yml`, `.env`, novos arquivos de provisioning do Grafana. Nenhuma mudança de código no oauth (nenhum FR alterado).

## 3. Recommended Approach

**Direct Adjustment** — novo epic adicionado ao plano existente, sem modificar épicos concluídos nem reabrir escopo do MVP original. Esforço baixo, risco baixo: mudança aditiva e isolada (serviço novo no compose, sem dependência de código do oauth nesta rodada).

## 4. Detailed Change Proposals

| Artefato | Mudança | Arquivo(s) |
|---|---|---|
| PRD | Nova seção 4.4 + NFR-7 | `prds/prd-oauth-2026-09-02/prd.md`, `planning-artifacts/prd.md` |
| Arquitetura | Nova AD-7 + linha na Stack Tecnológica | `architecture/architecture-oauth-2026-09-02/ARCHITECTURE-SPINE.md`, `planning-artifacts/architecture.md` |
| Epics | Novo Epic 5 + Story 5.1 | `planning-artifacts/epics.md` |
| Sprint Status | `epic-5` e `5-1-...` como `backlog` | `implementation-artifacts/sprint-status.yaml` |
| Infra (repo `base`) | Serviço `grafana` + volume `grafana-data` + variáveis de ambiente + provisioning de datasource/dashboard | `docker-compose.yml`, `.env`, `infrastructure/dev.local/services/grafana/provisioning/**` |

Todas as edições foram apresentadas e aprovadas individualmente pelo usuário (modo Incremental).

## 5. Implementation Handoff

- **Classificação:** Minor — implementado diretamente nesta sessão, sem necessidade de replanejamento.
- **Responsável:** Você (branch base `grupo01`, integrante 1).
- **Branch:** `grupo01-feat/grafana-observability`, criada a partir de `main` no repositório `base` (o docker-compose.yml e .env pertencem ao repo raiz, não ao submodule oauth).
- **Status:** Story 5.1 implementada (serviço Grafana provisionado e validado via `docker compose config`); `sprint-status.yaml` ainda marca `backlog` — atualizar para `done` após validação em ambiente real (`docker compose up -d grafana`) e revisão de código/commit.
- **Follow-up dependente (fora deste proposal):** frente do Prometheus (colega) — inclui tanto o serviço coletor quanto a instrumentação do oauth (endpoint `/metrics` na porta já reservada `OAUTH_INTERNAL_METRICS_PORT`), ambos sob a mesma responsabilidade.

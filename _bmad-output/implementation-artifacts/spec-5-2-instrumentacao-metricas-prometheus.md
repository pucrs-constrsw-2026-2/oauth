---
title: 'Story 5.2: Instrumentação de Métricas do OAuth e Exposição no Padrão Prometheus'
type: 'feature'
created: '2026-09-14'
status: 'done'
context:
  - '_bmad-output/planning-artifacts/prd.md'
  - '_bmad-output/planning-artifacts/architecture.md'
  - '_bmad-output/planning-artifacts/epics.md'
---

## Intent

**Problem:** O microserviço `oauth` não expunha métricas operacionais no formato OpenMetrics/Prometheus, impossibilitando que um servidor Prometheus coletor raspasse indicadores de saúde, tráfego HTTP, latência e uso de recursos para visualização em dashboards do Grafana.

**Approach:** Implementar um registry atômico de métricas Prometheus (`MetricsRegistry`), um event listener HTTP (`MetricsRequestListener`) para registrar tráfego e latência automaticamente em segundo plano, e um controller dedicado (`MetricsController`) expondo o endpoint `GET /metrics` com cabeçalho `Content-Type: text/plain; version=0.0.4; charset=utf-8`.

## Boundaries & Constraints

**Always:**
- O formato de exportação de métricas deve seguir estritamente a especificação OpenMetrics / Prometheus Exposition Format.
- Requisições ao próprio endpoint `/metrics` não devem inflar os contadores de requisições de negócio (`http_requests_total`).
- A persistência dos contadores deve suportar múltiplos processos ou requisições concorrentes sem corrupção de dados.
- O endpoint deve ser público para que o scraper Prometheus consiga acessá-lo sem necessidade de autenticação Bearer.

**Never:**
- Não expor dados sensíveis de credenciais de usuários nas labels das métricas.
- Não introduzir bloqueios de I/O pesados que degradem o throughput da API principal.

## Tasks & Acceptance

**Execution:**
- [x] `src/Infrastructure/Metrics/MetricsRegistry.php` -- Registry para gerenciamento de contadores, medidores (gauges) e latência em formato canônico OpenMetrics.
- [x] `src/Infrastructure/Http/Listener/MetricsRequestListener.php` -- Event listener de requisição/resposta para coletar automaticamente tráfego HTTP e duração.
- [x] `src/Infrastructure/Http/Controller/MetricsController.php` -- Controller expondo `GET /metrics`.
- [x] `src/Infrastructure/OpenApi/OpenApiSpecificationBuilder.php` -- Documentação do endpoint `/metrics` no OpenAPI/Swagger.
- [x] `tests/Unit/Infrastructure/Metrics/MetricsRegistryTest.php` -- Testes unitários do registry.
- [x] `tests/Integration/Infrastructure/Http/Controller/MetricsControllerTest.php` -- Testes de integração do endpoint e do listener de tráfego.
- [x] `tests/Integration/Infrastructure/Http/Controller/SwaggerControllerTest.php` -- Validação da rota `/metrics` na especificação OpenAPI.

**Acceptance Criteria:**
- Given o microserviço `oauth` em execução
- When uma requisição `GET /metrics` for enviada
- Then a API responde HTTP 200 (OK) com header `Content-Type: text/plain; version=0.0.4; charset=utf-8`
- And o corpo da resposta contém as famílias de métricas do runtime PHP (`php_info`, `php_memory_bytes`, `php_memory_peak_bytes`)
- And o corpo contém os metadados do serviço (`oauth_service_info`) com labels de serviço, framework e realm
- And requisições HTTP anteriores são contabilizadas em `http_requests_total` com labels `method`, `route` e `status`
- And a duração das requisições é registrada em `http_request_duration_seconds`
- And requisições ao próprio `/metrics` não inflam as métricas de tráfego de negócio.

## Verification

**Commands:**
- `vendor/bin/phpunit` -- expected: 95 testes passando, 431 asserções OK.
- `curl -s http://localhost:8181/metrics` -- expected: HTTP 200 com payload Prometheus válido contendo `php_info`, `php_memory_bytes`, `oauth_service_info` e `http_requests_total`.

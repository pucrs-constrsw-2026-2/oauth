# Especificação de Observabilidade e Monitoramento - Serviço OAuth

Este documento especifica os padrões, indicadores e ferramentas de observabilidade e telemetria implementados no serviço **OAuth** (`oauth-api`) utilizando **OpenTelemetry**, **Micrometer** e **Prometheus**, em conformidade com os requisitos de arquitetura da disciplina de Construção de Software 2026/2.

---

## 1. Serviços Monitorados

| Serviço | Nome da Aplicação (`spring.application.name`) | Descrição | Tecnologia |
|---|---|---|---|
| **OAuth API** | `oauth-api` | Serviço responsável pela validação de access tokens JWT emitidos pelo Keycloak e autorização de acesso a recursos com base em roles. | Spring Boot 3.4.2, Java 21, OpenTelemetry, Micrometer |

---

## 2. Arquitetura de Observabilidade

A observabilidade do serviço está estruturada sobre os três pilares da telemetria moderna:

```
+-------------------------------------------------------------+
|                         oauth-api                           |
|                                                             |
|  +--------------------+         +------------------------+  |
|  |     Micrometer     |         |  Micrometer Tracing    |  |
|  |     Prometheus     |         |      (OTel Bridge)     |  |
|  +---------+----------+         +-----------+------------+  |
+------------|--------------------------------|---------------+
             |                                |
             v                                v
     /actuator/prometheus               OTLP Exporter
    (Scrape HTTP: 9464)            (gRPC/HTTP: localhost:4318)
             |                                |
             v                                v
      [ Prometheus ]                 [ OTel Collector / ]
      [ & Grafana  ]                 [ Jaeger / Tempo   ]
```

1. **Métricas (Metrics)**:
   - Coletadas via **Micrometer** e expostas pelo **Spring Boot Actuator** em formato compatível com **Prometheus**.
   - Disponíveis para *scraping* através do endpoint HTTP `/actuator/prometheus`.

2. **Rastreamento Distribuído (Distributed Tracing)**:
   - Instrumentado via **Micrometer Tracing Bridge OpenTelemetry** e exportado via **OpenTelemetry OTLP Exporter**.
   - Padrão de propagação de contexto de rastreio: **W3C TraceContext** (`traceparent`, `tracestate`).
   - Taxa de amostragem configurada para 100% em desenvolvimento (`management.tracing.sampling.probability: 1.0`).

3. **Verificação de Saúde (Health Checks)**:
   - Expostos via endpoint HTTP `/actuator/health` e `/health`.

---

## 3. Portas e Endpoints de Exposição

| Finalidade | Porta | Endpoint | Protocolo | Descrição |
|---|---|---|---|---|
| **Aplicação (Negócio)** | `8081` | `/validate`, `/authorize` | HTTP | Endpoints de validação de tokens e autorização |
| **Health Check Básico** | `8081` | `/health` | HTTP | Liveness probe rápido da aplicação |
| **Métricas Prometheus** | `9464` | `/actuator/prometheus` | HTTP | Scraping de indicadores no formato Prometheus |
| **Actuator Health** | `9464` | `/actuator/health` | HTTP | Status detalhado da saúde dos componentes |
| **OpenTelemetry OTLP** | `4318` | `/v1/traces` | HTTP/OTLP | Envio de spans de trace para o collector |

*Nota: A porta `9464` é a porta dedicada de gerenciamento (`management.server.port`), isolando o tráfego de métricas do tráfego de negócio.*

---

## 4. Indicadores e Métricas Coletadas

### 4.1. Indicadores de Negócio (Custom Metrics - Domínio OAuth)

| Indicador (Métrica Prometheus) | Tipo | Tags / Dimensões | Descrição |
|---|---|---|---|
| `oauth_validations_total` | Counter | `status` (`allowed`, `forbidden`), `resource` | Total de requisições de validação de acesso processadas |
| `oauth_validations_denied_total` | Counter | `reason` (`missing_header`, `missing_resource`, `invalid_token`, `unauthorized_role`) | Total de acessos negados categorizados pelo motivo |
| `oauth_token_validation_duration_seconds` | Timer | `result` (`success`, `failure`) | Tempo gasto na decodificação e validação do token |

### 4.2. Indicadores HTTP e Servidor Web

| Indicador | Tipo | Descrição |
|---|---|---|
| `http_server_requests_seconds_count` | Counter | Quantidade total de requisições HTTP recebidas por rota e status |
| `http_server_requests_seconds_sum` | Counter | Tempo total acumulado de resposta das requisições HTTP |
| `http_server_requests_seconds_max` | Gauge | Tempo máximo de resposta observado na janela recente |

### 4.3. Indicadores de Infraestrutura e JVM

| Indicador | Tipo | Descrição |
|---|---|---|
| `jvm_memory_used_bytes` | Gauge | Memória JVM utilizada (Heap e Non-Heap) |
| `jvm_memory_max_bytes` | Gauge | Limite máximo de memória configurado na JVM |
| `jvm_threads_live_threads` | Gauge | Quantidade de threads ativas no processo |
| `process_cpu_usage` | Gauge | Percentual de uso de CPU do processo Java |
| `system_cpu_usage` | Gauge | Percentual de uso de CPU de todo o sistema operacional |
| `jvm_gc_pause_seconds_count` | Counter | Contagem de pausas para Garbage Collection |

---

## 5. Dependências Declaradas no `pom.xml`

As dependências responsáveis pela geração e exportação desses indicadores são:

```xml
<!-- Observability: Actuator, Metrics & OpenTelemetry -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

---

## 6. Validação e Testes Automatizados

O serviço conta com testes automatizados garantindo o funcionamento do domínio e da observabilidade:
- **Testes Unitários**:
  - `KeycloakServiceTest`: Validação da matriz de permissões por role e normalização de recursos.
  - `AuthControllerTest`: Validação unitária do controller com simulação de tokens e respostas HTTP.
- **Testes de Integração**:
  - `AuthControllerIntegrationTest`: Validação das chamadas HTTP completas (`/validate` e `/authorize`).
  - `ObservabilityIntegrationTest`: Validação da geração de métricas pelo Actuator (`/actuator/health` e `/actuator/prometheus`) e incremento dos contadores de indicadores.

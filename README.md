# oauth

Serviço OAuth - constrsw 2026/2

Microsserviço responsável pela autenticação e autorização baseada em tokens JWT (emitidos pelo Keycloak) e pelo controle de acesso por papéis (roles) aos recursos do sistema acadêmico.

## Observabilidade e Monitoramento

Este serviço é monitorado através de métricas (**Prometheus**) e rastreamento distribuído (**OpenTelemetry**).

A especificação completa de todos os indicadores, portas, endpoints e ferramentas está detalhada em:
👉 [ESPECIFICACAO_OBSERVABILIDADE.md](./ESPECIFICACAO_OBSERVABILIDADE.md)

### Endpoints Principais
- **Validação de Acesso**: `POST /validate` ou `GET /validate?resource={nome}` (porta `8081`)
- **Autorização**: `POST /authorize` ou `GET /authorize?resource={nome}` (porta `8081`)
- **Health Check**: `GET /health` (porta `8081`)
- **Métricas Prometheus**: `GET /actuator/prometheus` (porta `9464`)
- **Health Check Detalhado**: `GET /actuator/health` (porta `9464`)

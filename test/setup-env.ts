// Valores padrao para rodar os testes de INTEGRACAO localmente, fora do
// docker compose: a API roda no processo do jest (no host), nao dentro da
// rede interna do container, entao usa as portas EXTERNAS do Keycloak
// (ver docker-compose.yml / .env da raiz do repo base).
//
// So define o que ainda nao estiver setado no ambiente - se voce exportar
// essas variaveis manualmente (ou rodar via docker/CI com outro valor),
// isso aqui nao sobrescreve.
//
// Pre-requisito pra esses testes passarem: `docker compose up -d` rodando
// na raiz do repo base, com o Keycloak saudavel e o realm "constrsw"
// importado (usuarios de teste do professor: admin@pucrs.br, etc).
process.env.KEYCLOAK_SERVER_URL ??= 'http://localhost:8081';
process.env.KEYCLOAK_REALM ??= 'constrsw';
process.env.KEYCLOAK_CLIENT_ID ??= 'oauth';
process.env.KEYCLOAK_CLIENT_SECRET ??= 'wsNXUxaupU9X6jCncsn3rOEy6PDt7oJO';

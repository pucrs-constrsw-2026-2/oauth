# OAuth

Serviço OAuth do grupo 08 para ConstrSW 2026/2.

## Execução com o repositório base

O `docker-compose.yml` compartilhado da raiz atualmente verifica o serviço
OAuth com Node.js. Como esta implementação usa Java, execute a partir da raiz
do repositório `base` incluindo o override desta branch:

```bash
docker compose \
  -f docker-compose.yml \
  -f backend/oauth/docker-compose.base.override.yml \
  up -d --build keycloak oauth
```

O override altera somente o health check do serviço `oauth`. Portas, volumes,
variáveis e os demais serviços continuam sendo definidos pelo compose oficial.

Após a inicialização:

- Health: `http://localhost:8181/health`
- Swagger: `http://localhost:8181/docs`

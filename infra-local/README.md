# infra-local (patches locais para o compose do professor)

Esta pasta **não faz parte da API `oauth`** em si — é infraestrutura auxiliar
para rodar o `docker-compose.yml` oficial do professor localmente em Mac com
Apple Silicon recente (M4), sem depender de nenhuma alteração na `base`
(onde o grupo03 não tem permissão de commit).

## Problemas contornados

1. **SIGILL da JVM do Keycloak** — bug conhecido do OpenJDK 21 em macOS
   15.2+ com Apple Silicon recente, relacionado a detecção de SVE. Ver
   [keycloak#36008](https://github.com/keycloak/keycloak/issues/36008) e
   JDK-8345296. Contornado com a env var `JAVA_TOOL_OPTIONS=-XX:UseSVE=0`.
2. **Healthcheck do Keycloak depende de `curl`** — a imagem oficial do
   Keycloak 26 não tem `curl` nem `dnf`/`microdnf` para instalá-lo (o
   Dockerfile do professor contornava isso sobrepondo o rootfs de uma imagem
   `redhat/ubi9`, o que por sua vez é a causa do problema 1 acima, em Mac
   Apple Silicon). Contornado com um healthcheck via `/dev/tcp` (builtin do
   bash) em vez de `curl`.

## Como usar

A partir da raiz do repo `base` (T1):

```bash
docker compose -f docker-compose.yml -f backend/oauth/docker-compose.override.yml up -d --build
```

Isso soma o `docker-compose.override.yml` (que mora aqui, dentro de
`backend/oauth`) por cima do `docker-compose.yml` oficial, sem alterar esse
último. O `docker-compose.yml` e o `Dockerfile` oficiais do professor
(`infrastructure/dev.local/services/keycloak/Dockerfile`) continuam
intocados.

## Reportado ao professor

Ambos os problemas foram reportados como bugs de ambiente (Docker Desktop +
macOS + Apple Silicon recente), não como erro na configuração dele — os
outros grupos com Mac M4 (ou similar) provavelmente vão precisar do mesmo
contorno.

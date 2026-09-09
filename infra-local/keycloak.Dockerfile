# Dockerfile alternativo do Keycloak, usado SO localmente (Apple Silicon) via
# docker-compose.override.yml deste mesmo diretorio (backend/oauth). Nao
# mexe no Dockerfile oficial do professor (infrastructure/dev.local/services/
# keycloak/Dockerfile) - fica tudo aqui dentro do oauth, que e o unico lugar
# onde o grupo03 tem permissao de commit.
#
# Motivo: o Dockerfile oficial sobrescreve o rootfs da imagem com o de uma
# imagem redhat/ubi9:9.3 so pra ganhar o curl (necessario pro healthcheck
# original). Isso corrompe bibliotecas nativas e derruba a JVM com SIGILL em
# Mac Apple Silicon. Aqui so copiamos o realm pra dentro da imagem oficial,
# sem tocar em mais nada dela - o healthcheck sem curl fica no
# docker-compose.override.yml, ao lado deste arquivo.
FROM quay.io/keycloak/keycloak:26.0.1

USER root
RUN mkdir -p /opt/keycloak/data/import && chown -R 1000:0 /opt/keycloak/data

COPY --chown=1000:0 constrsw.json /opt/keycloak/data/import/constrsw-realm.json

USER 1000

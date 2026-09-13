# OAuth API

API REST do grupo 02 para integração com o Keycloak do projeto Construção de Software 2026/2.

## Stack

- Java 21
- Spring Boot 3.5
- Maven
- Spring Web e Bean Validation
- Springdoc OpenAPI/Swagger

## Arquitetura

As rotas REST serão organizadas em camadas:

```text
Controller -> Service -> KeycloakClient -> Keycloak
```

O projeto não possui banco de dados: usuários e roles pertencem ao Keycloak.

## Execução local

```bash
mvn spring-boot:run
```

A aplicação usa a porta `3001` por padrão. As configurações do Keycloak são lidas das variáveis `KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID` e `KEYCLOAK_CLIENT_SECRET`.

## Docker Compose

Na raiz do repositório `base`:

```bash
docker volume create constrsw-keycloak-data
docker compose up --build
```

## URLs

- Saúde: <http://localhost:8181/health>
- Swagger: <http://localhost:8181/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8181/v3/api-docs>

## Testes

```bash
mvn test
```

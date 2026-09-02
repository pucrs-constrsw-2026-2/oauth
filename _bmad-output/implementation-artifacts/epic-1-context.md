# Epic 1 Context: Fundação do Microserviço, Ambiente Docker e Contratos da Arquitetura Hexagonal

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Estabelecer a infraestrutura de execução conteinerizada (PHP/Symfony, Nginx e Keycloak com provisionamento automático do realm), o esqueleto de diretórios da Arquitetura Hexagonal, o tratamento global de erros e a definição formal das Interfaces de Portas e DTOs na branch base `grupo01`.

## Stories

- Story 1.1: Configuração do Ambiente Docker e Provisionamento do Keycloak
- Story 1.2: Estrutura Base da Arquitetura Hexagonal, Interfaces de Portas e DTOs
- Story 1.3: Cliente HTTP Base do Keycloak e Interceptor Global de Erros JSON

## Requirements & Constraints

- Adoção estrita da Arquitetura Hexagonal (Ports & Adapters): a camada `Domain` é PHP 8.2+ puro e nunca deve depender de Symfony ou Keycloak.
- Execução completa e reproduzível via Docker Compose (`docker compose up -d`), expondo a API em `http://localhost:8000` e o Keycloak em `http://localhost:8080/auth`.
- Provisionamento automatizado do Keycloak sem passos manuais via `--import-realm` carregando `realm-export.json` na inicialização do container.
- O realm `constrsw` deve conter o client confidencial `oauth` (com secret), os roles (`administrator`, `coordinator`, `professor`, `student`), os recursos com suas URLs, as policies de role e as permissions associadas.
- Todas as interfaces de portas (`Inbound` e `Outbound`) e DTOs base devem estar pré-definidos na branch base `grupo01` para garantir que os outros 3 integrantes do grupo desenvolvam suas frentes em paralelo sem conflitos de merge.
- Autenticação administrativa com a Admin API do Keycloak via fluxo *Client Credentials* usando o client confidencial `oauth` com role `manage-users`.
- Respostas de erro padronizadas globalmente em formato JSON (`{ "error": { "code": "...", "message": "...", "details": [...] } }`).

## Technical Decisions

- **Stack:** PHP 8.2+ FPM, Symfony 6.4 LTS / 7.x, Nginx alpine, Keycloak 22+ (com `--import-realm` e path `/auth`).
- **Arquitetura:** `src/Domain` (Model, Port/Inbound, Port/Outbound, Exception), `src/Application` (DTO, UseCase), `src/Infrastructure` (Keycloak/Client, Keycloak/Adapter, Http/Controller, Http/Listener).
- **Git:** Branch base `grupo01` dentro de um repositório compartilhado (submódulo). Todas as branches de feature partirão de `grupo01` e farão PR para `grupo01`.

## Cross-Story Dependencies

- Story 1.1 provê o ambiente Docker e o Keycloak configurado.
- Story 1.2 cria os contratos de portas e modelos sobre a estrutura de pastas do Symfony.
- Story 1.3 implementa o cliente HTTP base que consome a configuração do Keycloak de 1.1 e implementa o listener de erros.
- As Stories 1.1, 1.2 e 1.3 formam a fundação mandatória para os Épicos 2, 3 e 4.

---
title: OAuth Microservice PRD
status: final
created: 2026-09-02
updated: 2026-09-02
---

# PRD: Microserviço de Autenticação e Autorização (OAuth/OIDC)

## 0. Document Purpose
Este documento formaliza os requisitos funcionais e não-funcionais do microserviço de Autenticação e Autorização para a disciplina de Construção de Software (2026/2). Ele serve como contrato único de escopo para o time de desenvolvimento composto por 4 integrantes, orienta o desenho da Arquitetura Hexagonal e estabelece critérios de aceitação rastreáveis com base na especificação acadêmica fornecida pelo professor.

## 1. Vision
Fornecer uma API REST padronizada, segura e resiliente desenvolvida em PHP com Symfony, atuando como uma camada de abstração e fachada sobre o Identity Provider (Keycloak). 
A solução evita que outros serviços da aplicação precisem se comunicar diretamente com o Keycloak ou se acoplar às suas APIs nativas. O microserviço engloba o ciclo completo de autenticação OAuth 2.0 / OpenID Connect (login, emissão e renovação de tokens), administração de contas de usuários e controle de acesso fino baseado em papéis (roles), recursos protegidos e políticas de autorização.

## 2. Target User & Casos de Uso

### 2.1 Jobs To Be Done
- **Como serviço consumidor (cliente):** Desejo autenticar usuários através de credenciais para obter Access Tokens e Refresh Tokens no padrão OIDC.
- **Como serviço consumidor (cliente):** Desejo validar se uma requisição portando um Access Token possui permissão para acessar determinado recurso do sistema acadêmico, sem precisar interagir com a complexidade interna do Keycloak.
- **Como administrador do sistema:** Desejo cadastrar, listar, consultar, atualizar atributos, alterar senhas e desativar contas de usuários através de endpoints REST padronizados.
- **Como integrante da equipe de desenvolvimento:** Desejo dispor de uma base Dockerizada pronta com a Arquitetura Hexagonal estruturada e contratos de interface (*Ports*) bem definidos, permitindo desenvolver meu módulo em uma branch isolada sem dependências bloqueantes ou conflitos de integração.

### 2.2 Non-Users (v1)
- Usuários finais interagindo por interfaces gráficas (a API é 100% REST/JSON; telas de login/portal de usuário pertencem ao frontend).
- Suporte a múltiplos Identity Providers simultâneos na versão v1 (a arquitetura desacopla a dependência via adaptadores, mas o adapter concreto inicial é exclusivo para Keycloak).

### 2.3 Key User Journeys
- **UJ-1: Autenticação de Usuário e Obtenção de Tokens**
  - **Persona + contexto:** Aluno, Professor ou Administrador acessa a aplicação informando suas credenciais corporativas (e-mail como username e senha).
  - **Entry state:** Não autenticado. Aplicação cliente encaminha requisição para a OAuth API.
  - **Path:** Cliente envia requisição para `POST /login`. A API consome o endpoint OIDC do Keycloak (`/protocol/openid-connect/token` com `grant_type: password`), processa o retorno e responde com os tokens emitidos.
  - **Climax:** Cliente recebe `access_token`, `refresh_token` e tempo de expiração (`expires_in`).
  - **Resolution:** Cliente armazena os tokens de forma segura para uso nas requisições seguintes.
  - **Edge case:** Credenciais inválidas ou conta desabilitada geram HTTP 401 (Unauthorized) com mensagem explicativa padronizada.

- **UJ-2: Renovação Transparente de Sessão Expirada**
  - **Persona + contexto:** Aplicação cliente detecta expiração do Access Token durante o fluxo de navegação do usuário.
  - **Entry state:** Access Token expirado, Refresh Token válido disponível.
  - **Path:** Cliente chama `POST /refresh` informando o `refresh_token`. A API aciona o Keycloak com `grant_type: refresh_token` e obtém um novo par de tokens.
  - **Climax:** Novos tokens são devolvidos com sucesso sem requerer nova digitação de senha pelo usuário.
  - **Resolution:** Continuidade transparente da sessão do usuário.
  - **Edge case:** Refresh Token expirado ou revogado resulta em HTTP 401, exigindo novo login.

- **UJ-3: Administração do Ciclo de Vida de Usuários**
  - **Persona + contexto:** Administrador autenticado gerencia contas de professores e alunos no sistema.
  - **Entry state:** Administrador possui Bearer Token válido.
  - **Path:** Admin invoca `POST /users` para cadastrar uma nova conta. Em seguida, pode listar contas ativas via `GET /users` ou consultar via `GET /users/{id}`. Caso o usuário saia da instituição, executa `DELETE /users/{id}`.
  - **Climax:** Novo usuário recebe ID único gerado pelo Keycloak; na exclusão, a conta tem seu status alterado para desabilitada (`enabled = false`).
  - **Resolution:** Registro histórico preservado no Keycloak, porém usuário impedido de realizar novos logins ou constar na listagem de usuários ativos.
  - **Edge case:** Tentativa de criar usuário com e-mail já existente resulta em HTTP 409 (Conflict). Consulta ou atualização de ID inexistente resulta em HTTP 404 (Not Found).

- **UJ-4: Validação de Acesso a Recurso Protegido (Policy Enforcement)**
  - **Persona + contexto:** Um microserviço acadêmico (ex.: serviço de Salas ou Aulas) recebe uma requisição e precisa garantir que o usuário logado pode acessar o recurso `rooms` ou `lessons`.
  - **Entry state:** Requisição possui header `Authorization: Bearer <access_token>`.
  - **Path:** O serviço consumidor chama `POST /authorize` passando o Bearer Token e o recurso visado no corpo da requisição (`{ "resource": "rooms" }`). A API valida a assinatura/validade do token e verifica se os papéis do usuário atendem às políticas de permissão cadastradas no Keycloak.
  - **Climax:** Se o usuário pertencer a um role com permissão associada (ex: `administrator` para `rooms`), a API responde HTTP 200 (OK).
  - **Resolution:** O microserviço consumidor autoriza a execução da operação solicitada.
  - **Edge case:** Se o usuário não possuir papel associado ao recurso (ex: `student` tentando acessar `rooms` ou `lessons`), a API responde HTTP 403 (Forbidden).

## 3. Glossary
- **Identity Provider (IdP):** Sistema centralizado responsável pela emissão de tokens, autenticação e gerenciamento de identidades (neste projeto: Keycloak).
- **Access Token:** Token assinado no padrão JWT contendo a identidade do usuário, prazos de validade e escopos/roles atribuídos.
- **Refresh Token:** Credencial de longa duração utilizada estritamente para renovação do Access Token sem requerer nova submissão de senha.
- **Realm:** Domínio isolado no Keycloak configurado para a aplicação (`constrsw`).
- **Client:** Registro da aplicação no Keycloak configurado em modo confidencial (`oauth`).
- **Roles:** Papéis de autorização atribuídos a usuários: `administrator`, `coordinator`, `professor`, `student`.
- **Resources:** Recursos protegidos do ecossistema acadêmico registrados no Keycloak: `classes`, `courses`, `lessons`, `professors`, `reservations`, `resources`, `rooms`, `students`.
- **Policies:** Regras de autorização associadas a papéis: `administrator-policy`, `coordinator-policy`, `professor-policy`.
- **Permissions:** Associação entre recursos e políticas:
  - `administrator-permissions`: recursos `resources`, `rooms`, `professors`, `students` vinculados à política `administrator-policy`.
  - `coordinator-permissions`: recursos `courses`, `classes` vinculados à política `coordinator-policy`.
  - `professor-permissions`: recursos `lessons`, `reservations` vinculados à política `professor-policy`.
- **Port (Porta):** Interface abstrata da camada de Domínio/Aplicação que define operações sem vincular detalhes técnicos.
- **Adapter (Adaptador):** Implementação de infraestrutura concreta de uma porta (ex: adaptador HTTP para a API do Keycloak).

## 4. Features & Functional Requirements

### 4.1 Autenticação & Gestão de Tokens
**Description:** Gerencia o fluxo de autenticação e o ciclo de vida dos tokens OAuth 2.0 / OpenID Connect. Realiza UJ-1 e UJ-2.

#### FR-1: Autenticação de Usuário (`POST /login`)
O sistema deve autenticar usuários enviando credenciais ao Keycloak e retornando os tokens OIDC correspondentes. Realiza UJ-1.
- **Consequences (testable):**
  - Requisição com credenciais válidas (`username` e `password`) retorna HTTP 200 (OK) com JSON contendo `token_type` ("Bearer"), `access_token`, `expires_in`, `refresh_token` e `refresh_expires_in`.
  - Aceita parâmetros enviados tanto via `application/json` quanto `application/x-www-form-urlencoded` / `multipart/form-data`.
  - Credenciais inválidas retornam HTTP 401 (Unauthorized).
  - Parâmetros obrigatórios ausentes retornam HTTP 400 (Bad Request).

#### FR-2: Renovação de Tokens (`POST /refresh`)
O sistema deve renovar tokens de acesso a partir de um refresh token válido. Realiza UJ-2.
- **Consequences (testable):**
  - Envio de `refresh_token` válido retorna HTTP 200 (OK) com novo par `access_token` e `refresh_token`.
  - Envio de `refresh_token` expirado, revogado ou malformado retorna HTTP 401 (Unauthorized).
  - Envio sem o campo `refresh_token` retorna HTTP 400 (Bad Request).

#### FR-3: Identificação do Usuário Logado (`GET /userinfo` ou `GET /me`)
O sistema deve fornecer os dados cadastrais do usuário autenticado a partir do token informado. Realiza UJ-1.
- **Consequences (testable):**
  - Chamada com header `Authorization: Bearer <valid_token>` consome o endpoint `/protocol/openid-connect/userinfo` do Keycloak e retorna HTTP 200 (OK) com perfil do usuário (`sub`, `name`, `email`, `preferred_username`, etc.).
  - Token inválido, expirado ou ausente retorna HTTP 401 (Unauthorized).

---

### 4.2 Gerenciamento de Usuários (User Management)
**Description:** Fornece endpoints REST para administração de usuários integrada à Keycloak Admin REST API. Todas as rotas exigem header `Authorization: Bearer <token>`. Realiza UJ-3.

#### FR-4: Criação de Usuário (`POST /users`)
Permite cadastrar uma nova conta de usuário no Keycloak. Realiza UJ-3.
- **Consequences (testable):**
  - Requisição com payload JSON válido cria o usuário e retorna HTTP 201 (Created) com os dados do usuário incluindo o `id` gerado automaticamente pelo Keycloak.
  - Tentativa de cadastro com e-mail já existente retorna HTTP 409 (Conflict).
  - Dados obrigatórios ausentes ou inválidos retornam HTTP 400 (Bad Request).

#### FR-5: Listagem de Usuários Habilitados (`GET /users`)
Permite recuperar todos os usuários cadastrados que estejam com a conta ativa. Realiza UJ-3.
- **Consequences (testable):**
  - Retorna HTTP 200 (OK) com lista JSON dos usuários ativos (`enabled: true`). Usuários desabilitados não constam no retorno padrão.
  - Token ausente ou inválido retorna HTTP 401 (Unauthorized).

#### FR-6: Consulta de Usuário por ID (`GET /users/{id}`)
Permite obter os detalhes cadastrais de um usuário específico. Realiza UJ-3.
- **Consequences (testable):**
  - ID localizado retorna HTTP 200 (OK) com a representação JSON do usuário.
  - ID inexistente retorna HTTP 404 (Not Found).

#### FR-7: Atualização de Atributos do Usuário (`PUT /users/{id}`)
Permite atualizar atributos cadastrais (nome, sobrenome, e-mail) do usuário. Realiza UJ-3.
- **Consequences (testable):**
  - Atualização bem-sucedida consome a Admin API do Keycloak via PUT e retorna HTTP 200 (OK) com corpo vazio.
  - ID inexistente retorna HTTP 404 (Not Found).
  - Payload inválido retorna HTTP 400 (Bad Request).

#### FR-8: Atualização de Senha do Usuário (`PATCH /users/{id}`)
Permite alterar a senha de um usuário existente. Realiza UJ-3.
- **Consequences (testable):**
  - Envio de JSON com novo atributo `password` atualiza a credencial via reset de senha na Admin API e retorna HTTP 200 (OK) com corpo vazio.
  - ID inexistente retorna HTTP 404 (Not Found).
  - Senha em formato inválido retorna HTTP 400 (Bad Request).

#### FR-9: Exclusão Lógica de Usuário (`DELETE /users/{id}`)
Desativa a conta de um usuário no Keycloak sem remoção física do registro. Realiza UJ-3.
- **Consequences (testable):**
  - ID localizado tem o atributo `enabled` atualizado para `false` no Keycloak e a API retorna HTTP 204 (No Content) com corpo vazio.
  - ID inexistente retorna HTTP 404 (Not Found).

---

### 4.3 Autorização e Validação de Políticas de Acesso
**Description:** Endpoint verificador de acesso a recursos baseado no token de acesso e nas regras de papéis e políticas configuradas. Realiza UJ-4.

#### FR-10: Validação de Acesso a Recurso (`POST /authorize`)
Valida o Access Token e avalia se os papéis do usuário autenticado conferem permissão de acesso ao recurso solicitado. Realiza UJ-4.
- **Consequences (testable):**
  - Requisição com header `Authorization: Bearer <valid_token>` e corpo JSON contendo `{"resource": "<resource_name>"}` retorna HTTP 200 (OK) caso o usuário possua permissão válida para o recurso conforme a matriz:
    * `administrator`: acesso concedido a `resources`, `rooms`, `professors`, `students`.
    * `coordinator`: acesso concedido a `courses`, `classes`.
    * `professor`: acesso concedido a `lessons`, `reservations`.
  - Caso o usuário não possua nenhum papel com permissão sobre o recurso solicitado, retorna HTTP 403 (Forbidden).
  - Caso o token esteja expirado, assinado de forma inválida ou ausente, retorna HTTP 401 (Unauthorized).
  - Caso o campo `resource` seja omitido ou inválido, retorna HTTP 400 (Bad Request).

---

## 5. Non-Goals (Explicit)
- Não implementação de telas ou interfaces com o usuário (UI) neste repositório.
- Não acoplamento da camada de domínio a SDKs específicos do Keycloak (as portas devem ser agnósticas).
- Não persistência direta de dados de usuários em bancos relacionais locais; a persistência canônica reside exclusivamente no Keycloak.
- Não execução de exclusão física (*hard delete*) de usuários (restringe-se a soft delete via `enabled = false`).

---

## 6. MVP Scope & Divisão de Trabalho em 4 Frentes

Para viabilizar a colaboração paralela entre os 4 integrantes do grupo sem conflitos de integração ou sobreposição de código:

### 6.1 Frente 1: Base Compartilhada & Fundação (Branch Base `grupo01` - Você)
- **Infraestrutura Docker:** Configuração de `docker-compose.yml` contendo os serviços PHP 8.2+ (Symfony), Nginx e Keycloak.
- **Automação do Keycloak:** Exportação/Importação automatizada de `realm-export.json` contendo o realm `constrsw`, client `oauth`, roles, resources, policies e permissions já provisionados ao subir o container.
- **Estrutura Hexagonal Base:**
  - `src/Domain`: Interfaces de Portas (`AuthPortInterface`, `UserManagementPortInterface`, `AuthorizationPortInterface`) e entidades/DTOs de domínio essenciais.
  - `src/Application`: Estrutura de casos de uso e DTOs de comando/consulta.
  - `src/Infrastructure`: Cliente HTTP base configurado com credenciais de serviço para comunicação com a Keycloak Admin API.
- **Tratamento de Erros:** Middleware/Event Listener global para padronização de respostas de erro em JSON.

### 6.2 Frente 2: Autenticação & Tokens (Branch `grupo01/feat/auth-tokens` - Integrante 2)
- Implementação de FR-1 (`POST /login`), FR-2 (`POST /refresh`) e FR-3 (`GET /userinfo` ou `/me`).
- Criação dos Use Cases correspondentes na camada de Aplicação.
- Implementação do adaptador concreto `KeycloakAuthAdapter` satisfazendo a porta `AuthPortInterface`.

### 6.3 Frente 3: Gestão de Usuários (Branch `grupo01/feat/user-management` - Integrante 3)
- Implementação de FR-4 (`POST /users`), FR-5 (`GET /users`), FR-6 (`GET /users/{id}`), FR-7 (`PUT /users/{id}`), FR-8 (`PATCH /users/{id}`) e FR-9 (`DELETE /users/{id}`).
- Criação dos Use Cases de criação, consulta, atualização e exclusão lógica.
- Implementação do adaptador `KeycloakUserManagementAdapter` satisfazendo a porta `UserManagementPortInterface`.

### 6.4 Frente 4: Autorização & Políticas (Branch `grupo01/feat/authorization-policies` - Integrante 4)
- Implementação de FR-10 (`POST /authorize`).
- Criação do Use Case de autorização e decodificação/avaliação de papéis versus recursos.
- Implementação do adaptador `KeycloakAuthorizationAdapter` satisfazendo a porta `AuthorizationPortInterface`.

---

## 7. Success Metrics & Critérios de Aceitação
- **SM-1:** 100% dos 10 requisitos funcionais (FR-1 a FR-10) operacionais e validados com os códigos HTTP definidos.
- **SM-2:** O ambiente completo deve subir e ficar pronto para uso através do comando `docker compose up -d`, sem necessidade de passos manuais de configuração inicial no Keycloak.
- **SM-3 (Arquitetura):** A camada de Domínio deve ser 100% isolada; nenhuma dependência ou import de bibliotecas específicas do Keycloak deve estar presente fora de `src/Infrastructure`.
- **SM-C1 (Contra-métrica):** A branch principal de fundação não deve implementar controladores de negócio dos outros integrantes, assegurando contribuições equilibradas e commits independentes para os 4 integrantes.

---

## 8. Open Questions & Decisões Registradas
1. **Comunicação Administrativa com o Keycloak:** A API consumirá a Keycloak Admin API através de autenticação via *Client Credentials* configurada no client `oauth` com papéis de service account (`manage-users`), garantindo que chamadas administrativas não dependam do token de login do usuário comum.
2. **Compatibilidade de Rotas do Keycloak:** O provisionamento do Keycloak suportará o prefixo `/auth/` indicado na especificação do professor, garantindo conformidade com os exemplos fornecidos.

---

## 9. Assumptions Index
- `[ASSUMPTION-1]`: O endpoint de login suportará tanto `application/json` quanto `application/x-www-form-urlencoded` / `multipart/form-data`.
- `[ASSUMPTION-2]`: O endpoint de autorização receberá o recurso solicitado no corpo JSON `{"resource": "<resource_name>"}` e o token de acesso via header `Authorization: Bearer <token>`.
- `[ASSUMPTION-3]`: A exclusão de usuário é estritamente lógica (`enabled = false`), preservando a integridade das identidades no Identity Provider.

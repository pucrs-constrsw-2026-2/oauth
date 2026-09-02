# PRD: Microserviço de Autenticação e Autorização (OAuth/OIDC)

**Status:** Final
**Criado:** 2026-09-02
**Versão:** 1.0
**Autor:** [Seu Nome] / Equipe [Nome da Equipe]

---

## 1. Visão do Produto

Este documento descreve os requisitos para o microserviço de Autenticação e Autorização, que atuará como uma camada de fachada entre a aplicação e o Identity Provider (Keycloak). O objetivo é centralizar e padronizar o fluxo de autenticação e autorização, seguindo o modelo de arquitetura Hexagonal e os padrões OAuth 2.0 / OpenID Connect (OIDC).

O microserviço será responsável por:
- Gerenciar o ciclo de vida dos tokens (emissão, renovação e revogação).
- Fornecer endpoints para cadastro e gerenciamento de usuários.
- Validar permissões de acesso a recursos (autorização).
- Isolar a lógica de negócio das especificidades de implementação do Keycloak.

---

## 2. Usuários-Alvo e Jornadas

### 2.1 Usuários
- **Usuário Final:** Interage indiretamente, através de clientes autenticados (frontends, outros microserviços).
- **Administrador do Sistema:** Responsável por gerenciar usuários e configurações de segurança.
- **Cliente (Microserviço Consumidor):** Outros serviços da aplicação que precisam validar tokens ou obter informações do usuário.

### 2.2 Jobs To Be Done

**Para o Usuário Final:**
- **Obter credenciais seguras:** Acessar o sistema informando suas credenciais (login/senha).
- **Manter sessão ativa:** Ter sua identidade validada automaticamente sem interrupções.
- **Acessar recursos:** Ter acesso garantido aos recursos para os quais possui permissão.

**Para o Administrador:**
- **Gerenciar usuários:** Cadastrar, atualizar, desativar e excluir contas de usuários.
- **Controlar acesso:** Definir políticas de permissão baseadas em papéis (roles) e recursos.
- **Auditar atividades:** Monitorar eventos de autenticação e segurança.

**Para o Cliente/Microserviço:**
- **Validar autenticação:** Verificar a validade de um token de acesso.
- **Obter perfil do usuário:** Recuperar informações do usuário logado (e-mail, nome, papéis).
- **Enforcar políticas:** Verificar se um usuário tem permissão para acessar um recurso específico.

### 2.3 User Journeys

**UJ-1: Autenticação e Obtenção de Tokens**
1. Usuário insere credenciais (email/senha) no cliente.
2. Cliente envia POST para `/login` com `grant_type=password`.
3. OAuth Service valida com Keycloak.
4. Retorna `access_token`, `refresh_token` e `expires_in`.
5. Cliente armazena tokens de forma segura.

**UJ-2: Renovação de Token Expirado**
1. Cliente detecta expiração do `access_token`.
2. Cliente envia POST para `/refresh` com `refresh_token`.
3. OAuth Service renova com Keycloak.
4. Retorna novo par de tokens.

**UJ-3: Cadastro e Gerenciamento de Usuário**
1. Administrador acessa `/users` e envia POST com dados do novo usuário.
2. OAuth Service cria usuário no Keycloak.
3. (Opcional) Administrador atualiza ou desativa usuário via PUT/DELETE em `/users/{id}`.

**UJ-4: Validação de Autorização**
1. Cliente envia POST para `/authorize` com `access_token` e `resource`.
2. OAuth Service valida token e verifica permissões via Keycloak API.
3. Retorna `allowed=true` ou `allowed=false` com detalhes do motivo.

---

## 3. Requisitos Funcionais

| ID | Requisito | Justificativa | Prioridade |
|----|-----------|---------------|-----------|
| **RF-01** | O sistema deve suportar o fluxo de autenticação OIDC com `grant_type=password`. | Essencial para login de usuários. | Alta |
| **RF-02** | O sistema deve suportar o fluxo de renovação de tokens (`grant_type=refresh_token`). | Essencial para manter sessões sem re-login. | Alta |
| **RF-03** | O sistema deve fornecer endpoints para CRUD completo de usuários (criação, leitura, atualização, exclusão). | Permite administração do sistema. | Alta |
| **RF-04** | O sistema deve validar tokens de acesso via introspecção ou validação de assinatura JWT. | Essencial para segurança. | Alta |
| **RF-05** | O sistema deve validar permissões de acesso a recursos com base nas políticas do Keycloak. | Essencial para autorização. | Alta |
| **RF-06** | O sistema deve retornar informações do usuário (perfil) a partir do token de acesso. | Essencial para personalização. | Média |
| **RF-07** | O sistema deve permitir atualização de senha de usuários. | Essencial para gerenciamento de contas. | Média |
| **RF-08** | O sistema deve desabilitar usuários, impedindo novos logins. | Essencial para revogação de acesso. | Média |
| **RF-09** | O sistema deve centralizar logs de eventos de segurança. | Essencial para auditoria. | Média |
| **RF-10** | O sistema deve suportar múltiplas audiências (audiences) para tokens. | Essencial para isolamento entre serviços. | Baixa |

---

## 4. Requisitos Não-Funcionais

| ID | Categoria | Requisito | Testável? |
|----|-----------|-----------|----------|
| **RNF-01** | **Desempenho** | O tempo de resposta para autenticação deve ser < 500ms. | Sim |
| **RNF-02** | **Desempenho** | O tempo de resposta para validação de token deve ser < 100ms. | Sim |
| **RNF-03** | **Disponibilidade** | O sistema deve operar com 99.5% de uptime. | Sim |
| **RNF-04** | **Segurança** | Segredos de cliente nunca devem ser expostos em logs. | Sim |
| **RNF-05** | **Segurança** | Comunicação com Keycloak deve usar TLS (HTTPS). | Sim |
| **RNF-06** | **Segurança** | Senhas de usuários não devem ser armazenadas em texto plano. | Sim |
| **RNF-07** | **Segurança** | Tokens devem seguir expiração curta (e.g., 5-15 minutos). | Sim |
| **RNF-08** | **Escalabilidade** | O sistema deve suportar até 1000 requisições/segundo (TPS). | Sim |
| **RNF-09** | **Manutenibilidade** | Código deve seguir Arquitetura Hexagonal com testes unitários (>80% de cobertura). | Sim |
| **RNF-10** | **Portabilidade** | Aplicação deve rodar em ambiente Docker e Kubernetes. | Sim |

---

## 5. Limitações e Restrições

- **Tecnológicas:**
  - Stack: PHP 8.2+ com Symfony 6+.
  - Banco de dados: Apenas para logs (opcional); autenticação primária é no Keycloak.
  - Infraestrutura: Docker e Kubernetes.
- **Negócio:**
  - Escopo limitado à autenticação e autorização (não inclui UI de login).
  - Dependência obrigatória do Keycloak como Identity Provider.
- **Tempo:**
  - Prazo de entrega definido pelo calendário acadêmico.
  - MVP deve conter apenas funcionalidades essenciais.

---

## 6. Critérios de Aceitação

**Critério 1: Autenticação Funcional**
- Dado que o usuário fornece credenciais válidas,
- Quando ele envia uma requisição para `/login`,
- Então o sistema deve retornar um `access_token` e `refresh_token` válidos.

**Critério 2: Renovação Transparente**
- Dado que o `access_token` expirou e o `refresh_token` é válido,
- Quando o sistema envia uma requisição para `/refresh`,
- Então um novo par de tokens deve ser retornado sem intervenção do usuário.

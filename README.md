# Microserviço de Autenticação e Autorização (OAuth/OIDC)

**Disciplina:** Construção de Software (2026/2) — PUCRS  
**Grupo:** Grupo 01  
**Branch Base do Grupo:** `grupo01`  
**Arquitetura:** Hexagonal (*Ports & Adapters*)  
**Stack:** PHP 8.2+ com Symfony 6.4 LTS, Nginx e Keycloak 22+ (Quarkus)  

---

## 👥 Integrantes do Grupo 01

* **Fernando Gazzana**
* **Luiz Agusto Kubaszewski**
* **Giancarlo Mena**
* **Vinicius Silva**

---

## 1. Visão Geral

Este microserviço atua como fachada de segurança e camada de autenticação e autorização para o ecossistema acadêmico, integrando-se nativamente com o **Keycloak** através dos padrões **OAuth 2.0** e **OpenID Connect (OIDC)**.

Adota a **Arquitetura Hexagonal**:
* **`src/Domain/`**: Regras de negócio puras (PHP 8.2+), modelos de domínio (`User`, `AuthTokens`, `Role`), exceções e **Interfaces de Portas** (`Inbound` e `Outbound`). Totalmente desacoplado de frameworks e de bibliotecas externas.
* **`src/Application/`**: Casos de uso (*UseCases*) e DTOs tipados.
* **`src/Infrastructure/`**: Tecnologias concretas e adaptadores. Contém os **Controllers do Symfony**, o **`KeycloakHttpClient`** (baseado em `Symfony\Contracts\HttpClient\HttpClientInterface`) e o **`JsonExceptionListener`** para padronização global de erros.

---

## 2. Como Rodar o Projeto com Docker

O ambiente é 100% conteinerizado e provisionado de forma automática com um único comando:

```bash
# 1. Clone ou garanta que está na branch base grupo01
git checkout grupo01
git pull origin grupo01

# 2. Crie o arquivo de ambiente a partir do template
cp .env.example .env

# 3. Suba todos os serviços (API Symfony + Nginx + Keycloak)
docker compose up -d
```

### Serviços Disponíveis:
* **API REST (Symfony / Nginx):** `http://localhost:8000`
  * Healthcheck: `GET http://localhost:8000/api/health`
* **Keycloak IdP:** `http://localhost:8080/auth`
  * **Usuário Admin:** `admin` | **Senha Admin:** `password123`
  * **Realm:** `constrsw` (carregado automaticamente via `--import-realm`)
  * **Client ID:** `oauth` (Client Secret configurado no `.env`)

---

## 3. Endpoints da API

| Método | Endpoint | Descrição | Frente Responsável |
| :---: | :--- | :--- | :---: |
| `GET` | `/api/health` | Healthcheck da API e do container | Fundação (Você) |
| `POST` | `/login` | Autenticação por usuário e senha (`grant_type: password`) | Frente 2 (Auth Tokens) |
| `POST` | `/refresh` | Renovação de sessão via `refresh_token` | Frente 2 (Auth Tokens) |
| `GET` | `/me` | Consulta dos dados do perfil autenticado (UserInfo) | Frente 2 (Auth Tokens) |
| `POST` | `/users` | Cadastro de novo usuário | Frente 3 (User Management) |
| `GET` | `/users` | Listagem de usuários ativos (`enabled: true`) | Frente 3 (User Management) |
| `GET` | `/users/{id}` | Consulta de usuário por ID | Frente 3 (User Management) |
| `PUT` | `/users/{id}` | Atualização de dados cadastrais | Frente 3 (User Management) |
| `PATCH` | `/users/{id}` | Atualização de senha de usuário | Frente 3 (User Management) |
| `DELETE` | `/users/{id}` | Exclusão lógica do usuário (`enabled: false`) | Frente 3 (User Management) |
| `POST` | `/authorize` | Validação de token e avaliação de políticas por recurso | Frente 4 (Authorization) |

---

## 4. Guia para os Integrantes do Grupo

Todas as diretrizes de desenvolvimento, regras de isolamento de branches (`grupo01/feat/...`) e abertura de Pull Requests estão documentadas em:
📄 [`docs/team-integration-guide.md`](docs/team-integration-guide.md)

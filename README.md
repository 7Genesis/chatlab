# ChatLab

[![CI](https://github.com/7Genesis/chatlab/actions/workflows/ci.yml/badge.svg)](https://github.com/7Genesis/chatlab/actions/workflows/ci.yml)
[![Licença: MIT](https://img.shields.io/badge/licença-MIT-blue.svg)](LICENSE)

Backend de um hub de atendimento multi-tenant: a ideia é reunir num só lugar as conversas que hoje ficam espalhadas por WhatsApp, redes sociais e e-mail. O projeto está **em desenvolvimento**; hoje ele cobre o cadastro de empresas (tenants) e o esquema do banco versionado.

## O que já funciona

- `POST /api/tenants` cria uma empresa e o usuário administrador dela **na mesma transação**: se algo falha, nada é gravado.
- A senha do administrador é guardada com hash BCrypt.
- O corpo é validado: nome da empresa (até 100 caracteres), nome e e-mail do administrador são obrigatórios, o e-mail precisa ser válido e a senha precisa ter pelo menos 8 caracteres. Dados inválidos respondem `400` com `{ "erro": "…" }`.
- `POST /api/auth/login` recebe e-mail e senha e devolve um token JWT (HS256) com `sub` (id do usuário), `tenantId`, `role` e `email`. Credenciais erradas, e-mail inexistente e usuário inativo respondem o mesmo `401`, sem revelar se o e-mail existe. O e-mail não diferencia maiúsculas de minúsculas.
- E-mail repetido responde `400` com uma mensagem clara (tratado em `GlobalExceptionHandler`), em vez de um erro 500.
- O esquema do banco é versionado com Flyway (`src/main/resources/db/migration`).

Exemplos:

```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{ "companyName": "Empresa Exemplo", "adminName": "Ana", "adminEmail": "ana@exemplo.com", "adminPassword": "troque-esta-senha" }'
```

Resposta (`201`):

```json
{ "id": "…", "companyName": "Empresa Exemplo", "active": true, "adminId": "…", "adminEmail": "ana@exemplo.com" }
```

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "ana@exemplo.com", "password": "troque-esta-senha" }'
```

Resposta (`200`):

```json
{ "token": "eyJ…", "tokenType": "Bearer", "expiresInSeconds": 3600 }
```

## Stack

Java 21, Spring Boot 4.1, Spring Data JPA (Hibernate 7), PostgreSQL 15, Flyway e Lombok.

## Como rodar

1. Tenha um PostgreSQL com o banco `chatlab_db` (usuário e senha em `src/main/resources/application.yml`).
2. Defina o segredo do JWT (a aplicação não sobe sem ele):

   ```bash
   export CHATLAB_JWT_SECRET=$(openssl rand -hex 32)
   ```

3. Rode:

   ```bash
   ./mvnw spring-boot:run
   ```

O Flyway cria as tabelas na primeira execução.

| Variável | Padrão | Uso |
|---|---|---|
| `CHATLAB_JWT_SECRET` | (obrigatória, mínimo de 32 caracteres) | segredo que assina o token |
| `CHATLAB_JWT_EXPIRATION_MINUTES` | `60` | validade do token |

## Testes

```bash
./mvnw verify
```

Os testes (`TenantApiTest`) sobem a aplicação com um **PostgreSQL de verdade**, via Testcontainers, então precisam de Docker. Eles cobrem a criação da empresa com o administrador, o hash BCrypt da senha (a resposta não devolve a senha nem o hash), o e-mail repetido (`400`, sem deixar uma empresa gravada pela metade) e a validação do corpo. O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda isso a cada push na `main` e a cada pull request.

## Próximos passos

- Proteger as rotas com o token: hoje o login existe, mas todas as rotas continuam liberadas (o `SecurityConfig` só desativa o CSRF).
- Mais testes: só o cadastro de empresas está coberto hoje.
- Canais de mensagem (WhatsApp, e-mail) e o histórico unificado de conversas, que é o objetivo do projeto.

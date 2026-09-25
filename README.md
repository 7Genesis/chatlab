# ChatLab

Backend de um hub de atendimento multi-tenant: a ideia é reunir num só lugar as conversas que hoje ficam espalhadas por WhatsApp, redes sociais e e-mail. O projeto está **em desenvolvimento**; hoje ele cobre o cadastro de empresas (tenants) e o esquema do banco versionado.

## O que já funciona

- `POST /api/tenants` cria uma empresa e o usuário administrador dela **na mesma transação**: se algo falha, nada é gravado.
- A senha do administrador é guardada com hash BCrypt.
- E-mail repetido responde `400` com uma mensagem clara (tratado em `GlobalExceptionHandler`), em vez de um erro 500.
- O esquema do banco é versionado com Flyway (`src/main/resources/db/migration`).

Exemplo:

```bash
curl -X POST http://localhost:8080/api/tenants \
  -H "Content-Type: application/json" \
  -d '{ "companyName": "Empresa Exemplo", "adminName": "Ana", "adminEmail": "ana@exemplo.com", "adminPassword": "troque-esta-senha" }'
```

Resposta (`201`):

```json
{ "id": "…", "companyName": "Empresa Exemplo", "active": true, "adminId": "…", "adminEmail": "ana@exemplo.com" }
```

## Stack

Java 21, Spring Boot 4.1, Spring Data JPA (Hibernate 7), PostgreSQL 15, Flyway e Lombok.

## Como rodar

1. Tenha um PostgreSQL com o banco `chatlab_db` (usuário e senha em `src/main/resources/application.yml`).
2. Rode:

   ```bash
   ./mvnw spring-boot:run
   ```

O Flyway cria as tabelas na primeira execução.

## Próximos passos

- Autenticação com JWT (hoje todas as rotas estão liberadas; o `SecurityConfig` só desativa o CSRF).
- Testes automatizados (ainda não há).
- Canais de mensagem (WhatsApp, e-mail) e o histórico unificado de conversas, que é o objetivo do projeto.

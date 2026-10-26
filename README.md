# ChatLab

[![CI](https://github.com/7Genesis/chatlab/actions/workflows/ci.yml/badge.svg)](https://github.com/7Genesis/chatlab/actions/workflows/ci.yml)
[![Licença: MIT](https://img.shields.io/badge/licença-MIT-blue.svg)](LICENSE)

Backend de um hub de atendimento multi-tenant: a ideia é reunir num só lugar as conversas que hoje ficam espalhadas por WhatsApp, redes sociais e e-mail. O projeto está **em desenvolvimento**; hoje ele cobre o cadastro de empresas (tenants), o login com JWT, rotas protegidas por token com isolamento por empresa e o esquema do banco versionado.

## Em resumo

- **Problema:** o atendimento de uma empresa fica espalhado por WhatsApp, redes sociais e e-mail. O ChatLab quer ser o hub único, para várias empresas (tenants) no mesmo backend.
- **Pronto hoje:** cadastro de empresa com o administrador numa só transação, login com JWT, rotas protegidas que filtram pelo tenant do token e esquema do banco versionado. Canais de mensagem ainda não existem.
- **Verificável:** 22 testes (19 de integração contra um PostgreSQL 15 real via Testcontainers e 3 unitários), rodados no CI a cada push na `main` e a cada pull request. Última execução local de `./mvnw verify` (JDK 25, compilando com `release 21`): 22 testes, 0 falhas, `BUILD SUCCESS`.
- **Isolamento por empresa:** só cadastro, login e health são públicos; as demais rotas exigem JWT válido e leem o tenant só da claim `tenantId` do token. Há testes com duas empresas. Ainda não há papéis nem autorização por perfil. Detalhes em [Isolamento multi-tenant](#isolamento-multi-tenant-o-que-existe-e-o-que-falta).

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

Listar os usuários da própria empresa (exige o token do login):

```bash
curl http://localhost:8080/api/users -H "Authorization: Bearer $TOKEN"
```

`GET /api/users/{id}` devolve um usuário da empresa do token; se o id for de outra empresa (ou não existir), responde `404`. Essas duas rotas existem para tornar o isolamento verificável, e a resposta traz `id`, `name`, `email`, `role` e `active` (nunca o hash da senha).

## Isolamento multi-tenant: o que existe e o que falta

Existe hoje:

- Todo usuário pertence a exatamente uma empresa: `users.tenant_id` tem chave estrangeira para `tenants(id)` (`ON DELETE CASCADE`) e índice próprio (`V1__create_initial_schema.sql`). Na entidade `User` a coluna é `nullable = false`.
- O token JWT carrega o `tenantId` do usuário, ao lado de `sub`, `role` e `email` (`JwtService`).
- O `SecurityConfig` é um resource server do Spring Security que valida o token com o mesmo `JwtDecoder` (mesma chave HS256) que assina os tokens do login. Públicos: `POST /api/tenants`, `POST /api/auth/login` e `/actuator/health`. Todo o resto exige token válido: sem token, com token malformado, assinado com outra chave, adulterado ou expirado, a resposta é `401`.
- O tenant do chamador vem somente da claim `tenantId` do token validado (`UserController`). Corpo, path e query não escolhem a empresa. `UserRepository` filtra por `tenantId` em `findByTenantId` e `findByIdAndTenantId`.
- Acesso por id a um usuário de outra empresa responde `404`, igual a um id inexistente, para não revelar que o registro existe.
- O cadastro cria empresa e administrador na mesma transação, então não existe tenant sem dono nem usuário sem tenant (coberto por `TenantApiTest`).
- `TenantIsolationApiTest` cria duas empresas com usuários e prova: sem token `401`; token inválido, de outra chave, adulterado ou expirado `401`; o token de A lista só usuários de A, o de B só os de B; o id de um usuário de B com o token de A (e o inverso) dá `404`; `?tenantId=` na query não muda o escopo; cadastro, login e `/actuator/health` seguem públicos.

Ainda não existe:

- Papéis e autorização por perfil: a claim `role` é emitida, mas nenhuma rota a consulta. Qualquer usuário autenticado da empresa lê os usuários dela.
- Filtro automático por tenant: o filtro é explícito em cada método do repositório, então cada consulta nova precisa lembrar de aplicá-lo. Ainda não há tabelas de conversas ou mensagens.
- Revogação de token: um token vale até expirar, mesmo que o usuário seja desativado depois do login.
- A claim `tenantId` é lida sem checagem extra; hoje todo token vem do `JwtService`, que sempre a preenche.

## Decisões técnicas

| Decisão | Por quê | Onde |
|---|---|---|
| Um esquema único com coluna `tenant_id` (em vez de um banco ou esquema por empresa) | Cadastrar uma empresa é um `INSERT` em transação, sem provisionar banco. O custo é que o isolamento depende de toda consulta filtrar por tenant. | `V1__create_initial_schema.sql`, `UserRepository` |
| Tenant lido só da claim `tenantId` do JWT validado, com `404` para recurso de outra empresa | Quem chama não escolhe a empresa, e o `404` não revela se o id existe em outra empresa. Usa o resource server que já estava no `pom.xml` e o `JwtDecoder` que já existia. | `SecurityConfig`, `UserController`, `TenantIsolationApiTest` |
| Empresa e administrador gravados com `@Transactional` e `cascade` | Se o e-mail já existe, o `INSERT` do usuário falha e a empresa não fica gravada pela metade. | `TenantService`, teste `emailRepetidoRespondeBadRequestENaoDeixaEmpresaSolta` |
| E-mail único no banco (`UNIQUE`), normalizado para minúsculas no cadastro e no login | A garantia fica na restrição do banco, não só na checagem da aplicação, o que vale também sob requisições concorrentes. Como o e-mail é único em todo o sistema, o login não precisa receber o tenant. Em troca, a mesma pessoa não pode ter o mesmo e-mail em duas empresas. | `V1`, `TenantService`, `AuthService`, `GlobalExceptionHandler` |
| Banco como fonte do esquema: Flyway cria as tabelas e `ddl-auto: validate` só confere | O Hibernate nunca altera o banco. Se a entidade e a migration divergirem, a aplicação não sobe. | `application.yml`, `db/migration` |
| Login devolve o mesmo `401` para e-mail inexistente, senha errada e usuário inativo | Não revela quais e-mails estão cadastrados. Quando o e-mail não existe, a senha é comparada com um hash falso para o tempo de resposta ficar parecido. (A resposta igual é testada; o tempo de resposta não foi medido.) | `AuthService`, `AuthApiTest` |
| Senha com BCrypt; a API responde com DTO, nunca com a entidade | Devolver a entidade entraria em loop (`Tenant` -> `User` -> `Tenant`) e exporia o hash. Os testes conferem que a resposta não traz senha nem hash. | `SecurityConfig`, `TenantController`, `TenantApiTest` |
| JWT HS256 com segredo vindo de variável de ambiente; a aplicação não sobe sem ele | Nenhum segredo fica no repositório, e um segredo ausente ou curto (menos de 32 caracteres) falha na inicialização, não em produção na primeira requisição. | `JwtProperties`, `JwtConfig`, `JwtPropertiesTest` |
| Testes de integração com PostgreSQL real (Testcontainers), sem banco em memória | As migrations do Flyway rodam como em produção, então um erro de SQL ou de mapeamento aparece nos testes. O custo é exigir Docker. | `IntegrationTestBase` |

## Estrutura

Pacotes em `src/main/java/com/chatlab/core`, organizados por domínio:

- `tenant`: empresa (`Tenant`), cadastro e `POST /api/tenants`.
- `user`: usuário, repositório e `GET /api/users`.
- `auth`: login, geração do JWT e configuração do segredo.
- `common`: tratamento central de erros (`GlobalExceptionHandler`).
- `config`: segurança (`SecurityConfig`): rotas públicas e validação do JWT.

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

São 22 testes. Os de integração sobem a aplicação com um **PostgreSQL 15 de verdade**, via Testcontainers, então precisam de Docker.

| Classe | Testes | O que cobre |
|---|---|---|
| `TenantApiTest` | 4 | criação da empresa com o administrador (a resposta não devolve senha nem hash), hash BCrypt no banco, e-mail repetido (`400`, sem deixar uma empresa gravada pela metade) e validação do corpo |
| `AuthApiTest` | 6 | login válido com conferência das claims do token (`sub`, `tenantId`, `role`), e-mail sem diferenciar maiúsculas, senha errada, e-mail inexistente (mesma resposta), usuário inativo e corpo inválido |
| `TenantIsolationApiTest` | 9 | duas empresas com usuários: `401` sem token e com token inválido, de outra chave, adulterado ou expirado; listagem e busca por id restritas à empresa do token; `404` para usuário de outra empresa; `tenantId` na query ignorado; cadastro, login e health públicos |
| `JwtPropertiesTest` | 3 | segredo ausente ou curto e validade inválida recusados na inicialização (unitário, sem Docker) |

Última execução local de `./mvnw verify` (JDK 25, compilando com `release 21`): 22 testes, 0 falhas, `BUILD SUCCESS`. O workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) roda isso a cada push na `main` e a cada pull request.

## Próximos passos

- Papéis e autorização por perfil: usar a claim `role`, que hoje é emitida mas não é consultada.
- Filtrar toda consulta nova por `tenant_id` quando surgirem as tabelas de conversas, com um teste de duas empresas para cada recurso novo.
- Canais de mensagem (WhatsApp, e-mail) e o histórico unificado de conversas, que é o objetivo do projeto.

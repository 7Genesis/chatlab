# ChatLab Core

Backend desenvolvido para o ecossistema ChatLab, focado em alta disponibilidade e arquitetura multitenant.

## Sobre o Projeto
O ChatLab é uma solução SaaS projetada para escalabilidade. O objetivo principal deste repositório é fornecer uma estrutura robusta de backend, utilizando boas práticas de desenvolvimento, padrões SOLID e uma arquitetura orientada a serviços.

## Desafio Técnico: Integridade de Persistência em JPA
Durante o desenvolvimento da camada de serviços, identificamos um conflito no ciclo de vida das entidades ao persistir um `Tenant` com seu usuário administrador. O Hibernate disparava o erro `StaleObjectStateException` devido ao gerenciamento de estado conflitante entre IDs gerados manualmente (via `UUID.randomUUID()`) e a expectativa do contexto de persistência.

**Solução aplicada:**
* Delegação da estratégia de geração de ID para o JPA (`@GeneratedValue`), eliminando a atribuição manual no código.
* Implementação da estratégia de cascata (`CascadeType.ALL`) no relacionamento `@OneToMany`.
* Garantia da atomicidade da transação via `@Transactional`, permitindo que o Hibernate gerencie a persistência de toda a árvore de objetos em uma única operação.

## Tecnologias
* Java 21
* Spring Boot 4.1.0
* Spring Data JPA / Hibernate 7
* PostgreSQL 15
* Flyway Migration
* Lombok

## Arquitetura
O sistema utiliza uma estrutura em camadas (Controller, Service, Repository, Domain) para garantir a separação de responsabilidades. A persistência é gerenciada via JPA, com suporte a relacionamentos bidirecionais e propagação de estado.

## Configuração de Ambiente
Certifique-se de ter o PostgreSQL rodando localmente com o banco `chatlab_db`.

1. Clone o repositório:
   `git clone https://github.com/7Genesis/chatlab.git`
2. Configure as variáveis de ambiente no `application.yml`:
   * `spring.datasource.url`: jdbc:postgresql://localhost:5432/chatlab_db
   * `spring.datasource.username`: [seu_usuario]
   * `spring.datasource.password`: [sua_senha]
3. Execute o projeto:
   `./mvnw spring-boot:run`

## Funcionalidades Implementadas
- [x] Estrutura base do projeto
- [x] Mapeamento de entidades (Tenant e User)
- [x] Suporte a persistência multitenant
- [x] Migrations automatizadas via Flyway

## Roadmap
- [ ] Implementação de autenticação JWT
- [ ] Criação de Response DTOs para serialização JSON e controle de dados sensíveis
- [ ] Testes de integração automatizados

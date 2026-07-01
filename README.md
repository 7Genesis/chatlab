# ChatLab Core

O **ChatLab** é a solução definitiva para empresas que sofrem com a dispersão de conversas em múltiplos canais. Nosso motor central unifica todo o histórico de atendimento em um único lugar, eliminando silos de informação e transformando conversas fragmentadas em ativos estratégicos para o negócio.

## O Problema: A Fragmentação da Comunicação
Empresas modernas enfrentam um gargalo crítico: a dispersão de dados em múltiplos canais (WhatsApp, redes sociais, e-mail). Esse cenário gera perda de histórico de atendimento e uma queda drástica na produtividade, forçando equipes a alternar entre diversas plataformas para gerenciar um único relacionamento.

## A Solução ChatLab
Desenvolvemos o ChatLab para ser o **ponto único de verdade** da comunicação empresarial.
- **Unificação Operacional:** Centralizamos todos os canais em uma única interface, permitindo foco total na qualidade da resposta.
- **Histórico Centralizado:** Garantimos que a informação flua de maneira íntegra, tornando cada conversa um ativo de conhecimento.
- **Escalabilidade Transacional:** Nossa arquitetura foi desenhada para suportar altos volumes de dados, garantindo que o negócio cresça sem falhas.

## Desafio Técnico: Integridade e Escalabilidade
Ao escalar o sistema, garantimos a atomicidade na criação de contas (Onboarding). Implementamos uma arquitetura robusta que assegura:
- **Consistência Total:** O uso de estratégias de cascata (`CascadeType.ALL`) e transações gerenciadas (`@Transactional`) garante que nenhum cliente inicie sua jornada com dados incompletos.
- **Eficiência de Código:** Refatoramos o ciclo de vida das entidades JPA, eliminando gargalos de persistência e garantindo que o sistema suporte o aumento da base de usuários com alta performance.

## Tecnologias
* **Java 21** e **Spring Boot 4.1.0**
* **PostgreSQL 15**
* **Spring Data JPA / Hibernate 7**
* **Flyway Migration**
* **Lombok**

## Configuração de Ambiente
1. Clone o repositório:
   `git clone https://github.com/7Genesis/chatlab.git`
2. Configure as variáveis no `application.yml`:
   ```yaml
   spring.datasource.url: jdbc:postgresql://localhost:5432/chatlab_db
   spring.datasource.username: [seu_usuario]
   spring.datasource.password: [sua_senha]
Execute: ./mvnw spring-boot:run

Roadmap
[x] Estrutura base e persistência multitenant

[ ] Implementação de segurança JWT (Em andamento)

[ ] Criação de Response DTOs para controle de dados sensíveis

[ ] Testes de integração automatizados

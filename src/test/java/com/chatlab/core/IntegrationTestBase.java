package com.chatlab.core;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base dos testes de integração: sobe a aplicação com um PostgreSQL de verdade (Testcontainers),
 * então as migrations do Flyway rodam como em produção. O container é um só para todas as classes.
 * Precisa de Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    static {
        postgres.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM tenants");
    }

    protected ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions criarEmpresa(String empresa, String nome, String email, String senha) throws Exception {
        return postJson("/api/tenants", """
                { "companyName": "%s", "adminName": "%s", "adminEmail": "%s", "adminPassword": "%s" }
                """.formatted(empresa, nome, email, senha));
    }
}

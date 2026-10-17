package com.chatlab.core.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integração: sobe a aplicação com um PostgreSQL de verdade (Testcontainers),
 * então as migrations do Flyway rodam como em produção. Precisa de Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TenantApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM tenants");
    }

    private static String corpo(String empresa, String nome, String email, String senha) {
        return """
                { "companyName": "%s", "adminName": "%s", "adminEmail": "%s", "adminPassword": "%s" }
                """.formatted(empresa, nome, email, senha);
    }

    @Test
    void criaEmpresaEAdminEDevolveSemExporASenha() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyName").value("Empresa Exemplo"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.adminEmail").value("ana@exemplo.com"))
                .andExpect(jsonPath("$.adminPassword").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void guardaASenhaComHashBCrypt() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1")))
                .andExpect(status().isCreated());

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'ana@exemplo.com'", String.class);

        assertThat(hash).isNotEqualTo("senha-forte-1").startsWith("$2");
    }

    @Test
    void emailRepetidoRespondeBadRequestENaoDeixaEmpresaSolta() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Empresa A", "Ana", "ana@exemplo.com", "senha-forte-1")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Empresa B", "Outra Ana", "ana@exemplo.com", "senha-forte-2")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("E-mail já cadastrado."));

        // A segunda tentativa falhou na mesma transação: a "Empresa B" não pode ter sido gravada.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(1);
    }

    @Test
    void recusaCorpoInvalido() throws Exception {
        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("", "Ana", "nao-e-email", "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isZero();
    }
}

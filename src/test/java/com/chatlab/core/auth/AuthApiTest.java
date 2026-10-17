package com.chatlab.core.auth;

import com.chatlab.core.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTestBase {

    @Autowired
    JwtDecoder jwtDecoder;

    private static String login(String email, String senha) {
        return "{ \"email\": \"%s\", \"password\": \"%s\" }".formatted(email, senha);
    }

    @Test
    void loginComCredenciaisCertasDevolveUmTokenValido() throws Exception {
        criarEmpresa("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1").andExpect(status().isCreated());

        String corpo = postJson("/api/auth/login", login("ana@exemplo.com", "senha-forte-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andReturn().getResponse().getContentAsString();

        Jwt jwt = jwtDecoder.decode(JsonPath.read(corpo, "$.token"));

        String userId = jdbc.queryForObject("SELECT id::text FROM users WHERE email = 'ana@exemplo.com'", String.class);
        String tenantId = jdbc.queryForObject("SELECT tenant_id::text FROM users WHERE email = 'ana@exemplo.com'", String.class);

        assertThat(jwt.getSubject()).isEqualTo(userId);
        assertThat(jwt.getClaimAsString("tenantId")).isEqualTo(tenantId);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ROLE_ADMIN");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void emailNaoDiferenciaMaiusculas() throws Exception {
        criarEmpresa("Empresa Exemplo", "Ana", "Ana@Exemplo.com", "senha-forte-1").andExpect(status().isCreated());

        postJson("/api/auth/login", login("ANA@exemplo.COM", "senha-forte-1")).andExpect(status().isOk());
    }

    @Test
    void senhaErradaRespondeUnauthorized() throws Exception {
        criarEmpresa("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1").andExpect(status().isCreated());

        postJson("/api/auth/login", login("ana@exemplo.com", "outra-senha"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void emailInexistenteRespondeIgualASenhaErrada() throws Exception {
        postJson("/api/auth/login", login("ninguem@exemplo.com", "senha-forte-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));
    }

    @Test
    void usuarioInativoNaoEntra() throws Exception {
        criarEmpresa("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1").andExpect(status().isCreated());
        jdbc.update("UPDATE users SET active = false WHERE email = 'ana@exemplo.com'");

        postJson("/api/auth/login", login("ana@exemplo.com", "senha-forte-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recusaCorpoSemEmailOuSenha() throws Exception {
        postJson("/api/auth/login", login("", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }
}

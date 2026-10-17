package com.chatlab.core.tenant;

import com.chatlab.core.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantApiTest extends IntegrationTestBase {

    @Test
    void criaEmpresaEAdminEDevolveSemExporASenha() throws Exception {
        criarEmpresa("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1")
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
        criarEmpresa("Empresa Exemplo", "Ana", "ana@exemplo.com", "senha-forte-1")
                .andExpect(status().isCreated());

        String hash = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'ana@exemplo.com'", String.class);

        assertThat(hash).isNotEqualTo("senha-forte-1").startsWith("$2");
    }

    @Test
    void emailRepetidoRespondeBadRequestENaoDeixaEmpresaSolta() throws Exception {
        criarEmpresa("Empresa A", "Ana", "ana@exemplo.com", "senha-forte-1")
                .andExpect(status().isCreated());

        criarEmpresa("Empresa B", "Outra Ana", "ana@exemplo.com", "senha-forte-2")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("E-mail já cadastrado."));

        // A segunda tentativa falhou na mesma transação: a "Empresa B" não pode ter sido gravada.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(1);
    }

    @Test
    void recusaCorpoInvalido() throws Exception {
        criarEmpresa("", "Ana", "nao-e-email", "curta")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isZero();
    }
}

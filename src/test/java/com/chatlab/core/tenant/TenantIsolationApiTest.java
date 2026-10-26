package com.chatlab.core.tenant;

import com.chatlab.core.IntegrationTestBase;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantIsolationApiTest extends IntegrationTestBase {

    @Autowired
    JwtEncoder jwtEncoder;

    String tokenA;
    String tokenB;
    String tenantIdA;
    String tenantIdB;
    String idUsuarioA;
    String idUsuarioB;

    @BeforeEach
    void criarDuasEmpresas() throws Exception {
        criarEmpresa("Empresa A", "Ana", "ana@a.com", "senha-forte-1").andExpect(status().isCreated());
        criarEmpresa("Empresa B", "Bia", "bia@b.com", "senha-forte-2").andExpect(status().isCreated());

        tenantIdA = jdbc.queryForObject("SELECT tenant_id::text FROM users WHERE email = 'ana@a.com'", String.class);
        tenantIdB = jdbc.queryForObject("SELECT tenant_id::text FROM users WHERE email = 'bia@b.com'", String.class);
        inserirUsuario(tenantIdA, "Alex", "alex@a.com");
        inserirUsuario(tenantIdB, "Beto", "beto@b.com");

        idUsuarioA = jdbc.queryForObject("SELECT id::text FROM users WHERE email = 'alex@a.com'", String.class);
        idUsuarioB = jdbc.queryForObject("SELECT id::text FROM users WHERE email = 'beto@b.com'", String.class);
        tokenA = login("ana@a.com", "senha-forte-1");
        tokenB = login("bia@b.com", "senha-forte-2");
    }

    private void inserirUsuario(String tenantId, String nome, String email) {
        jdbc.update("INSERT INTO users (id, tenant_id, name, email, password_hash) VALUES (gen_random_uuid(), ?::uuid, ?, ?, 'x')",
                tenantId, nome, email);
    }

    private String login(String email, String senha) throws Exception {
        String corpo = postJson("/api/auth/login", "{ \"email\": \"%s\", \"password\": \"%s\" }".formatted(email, senha))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private String tokenCom(JwtEncoder encoder, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chatlab").subject(idUsuarioA).issuedAt(emitidoEm).expiresAt(expiraEm)
                .claim("tenantId", tenantIdA).claim("role", "ROLE_ADMIN").build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void semTokenRespondeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/" + idUsuarioA)).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoRespondeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", bearer("isto-nao-e-um-jwt")))
                .andExpect(status().isUnauthorized());

        // Assinado com outro segredo.
        JwtEncoder outraChave = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec("outro-segredo-com-mais-de-32-caracteres-9876".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String falsificado = tokenCom(outraChave, Instant.now(), Instant.now().plusSeconds(600));
        mockMvc.perform(get("/api/users").header("Authorization", bearer(falsificado)))
                .andExpect(status().isUnauthorized());

        // Adulterado: a claim tenantId foi trocada mantendo a assinatura original.
        String[] partes = tokenA.split("\\.");
        String payloadB = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new String(java.util.Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                        .replace(tenantIdA, tenantIdB).getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(get("/api/users").header("Authorization", bearer(partes[0] + "." + payloadB + "." + partes[2])))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoRespondeUnauthorized() throws Exception {
        String expirado = tokenCom(jwtEncoder, Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600));

        mockMvc.perform(get("/api/users").header("Authorization", bearer(expirado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeAListaSoUsuariosDeA() throws Exception {
        String corpo = mockMvc.perform(get("/api/users").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        List<String> emails = JsonPath.read(corpo, "$[*].email");
        assertThat(emails).containsExactlyInAnyOrder("ana@a.com", "alex@a.com");
    }

    @Test
    void tokenDeBListaSoUsuariosDeB() throws Exception {
        String corpo = mockMvc.perform(get("/api/users").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        List<String> emails = JsonPath.read(corpo, "$[*].email");
        assertThat(emails).containsExactlyInAnyOrder("bia@b.com", "beto@b.com");
    }

    @Test
    void buscaPorIdDaMesmaEmpresaFunciona() throws Exception {
        mockMvc.perform(get("/api/users/" + idUsuarioA).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@a.com"));
    }

    @Test
    void idDeUsuarioDeOutraEmpresaRespondeNotFound() throws Exception {
        mockMvc.perform(get("/api/users/" + idUsuarioB).header("Authorization", bearer(tokenA)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users/" + idUsuarioA).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantDaQueryNaoMudaOEscopo() throws Exception {
        mockMvc.perform(get("/api/users").param("tenantId", tenantIdB).header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.email == 'bia@b.com')]").isEmpty());
    }

    @Test
    void cadastroLoginEHealthContinuamPublicos() throws Exception {
        criarEmpresa("Empresa C", "Carla", "carla@c.com", "senha-forte-3").andExpect(status().isCreated());
        postJson("/api/auth/login", "{ \"email\": \"carla@c.com\", \"password\": \"senha-forte-3\" }")
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}

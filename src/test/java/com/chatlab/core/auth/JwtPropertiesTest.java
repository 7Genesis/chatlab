package com.chatlab.core.auth;

import com.chatlab.core.auth.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final String SEGREDO_OK = "0123456789abcdef0123456789abcdef";

    @Test
    void aceitaSegredoDe32Caracteres() {
        assertThat(new JwtProperties(SEGREDO_OK, 60).expirationMinutes()).isEqualTo(60);
    }

    @Test
    void recusaSegredoAusenteOuCurto() {
        assertThatThrownBy(() -> new JwtProperties(null, 60)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties("", 60)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtProperties("curto", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHATLAB_JWT_SECRET");
    }

    @Test
    void recusaValidadeZeroOuNegativa() {
        assertThatThrownBy(() -> new JwtProperties(SEGREDO_OK, 0)).isInstanceOf(IllegalStateException.class);
    }
}

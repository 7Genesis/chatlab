package com.chatlab.core.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do token JWT. O segredo vem de variável de ambiente (CHATLAB_JWT_SECRET) e precisa
 * ter pelo menos 32 caracteres (HS256).
 */
@ConfigurationProperties(prefix = "chatlab.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Defina CHATLAB_JWT_SECRET com pelo menos 32 caracteres (por exemplo: openssl rand -hex 32).");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("chatlab.jwt.expiration-minutes deve ser maior que zero.");
        }
    }
}

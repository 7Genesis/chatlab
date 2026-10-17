package com.chatlab.core.auth.service;

import com.chatlab.core.auth.config.JwtProperties;
import com.chatlab.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtProperties props;

    public record Token(String valor, Duration validade) {}

    /** Gera o token do usuário: o id vai em `sub`, e o tenant e o papel vão como claims. */
    public Token gerar(User user) {
        Instant agora = Instant.now();
        Duration validade = Duration.ofMinutes(props.expirationMinutes());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chatlab")
                .subject(user.getId().toString())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade))
                .claim("tenantId", user.getTenant().getId().toString())
                .claim("role", user.getRole())
                .claim("email", user.getEmail())
                .build();

        String valor = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Token(valor, validade);
    }
}

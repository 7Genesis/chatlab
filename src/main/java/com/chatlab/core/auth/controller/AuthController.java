package com.chatlab.core.auth.controller;

import com.chatlab.core.auth.service.AuthService;
import com.chatlab.core.auth.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtService.Token token = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new LoginResponse(token.valor(), "Bearer", token.validade().toSeconds()));
    }
}

record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório.") @Email(message = "O e-mail é inválido.") String email,
        @NotBlank(message = "A senha é obrigatória.") String password) {}

record LoginResponse(String token, String tokenType, long expiresInSeconds) {}

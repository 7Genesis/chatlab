package com.chatlab.core.user.controller;

import com.chatlab.core.user.domain.User;
import com.chatlab.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Usuários da empresa do token. O tenant vem só da claim `tenantId` do JWT validado; usuário de
 * outra empresa responde 404, igual a um id que não existe.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return userRepository.findByTenantId(tenantId(jwt)).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return userRepository.findByIdAndTenantId(id, tenantId(jwt))
                .map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tenantId"));
    }
}

record UserResponse(UUID id, String name, String email, String role, Boolean active) {
    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getActive());
    }
}

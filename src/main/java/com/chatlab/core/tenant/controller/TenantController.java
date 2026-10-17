package com.chatlab.core.tenant.controller;

import com.chatlab.core.tenant.domain.Tenant;
import com.chatlab.core.tenant.service.TenantService;
import com.chatlab.core.user.domain.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantRequest request) {
        Tenant createdTenant = tenantService.createTenantWithAdmin(
                request.companyName(),
                request.adminName(),
                request.adminEmail(),
                request.adminPassword()
        );
        User admin = createdTenant.getUsers().get(0);

        // Responde com um DTO: devolver a entidade entra em loop (Tenant -> User -> Tenant) e exporia o hash da senha
        TenantResponse response = new TenantResponse(
                createdTenant.getId(),
                createdTenant.getCompanyName(),
                createdTenant.getActive(),
                admin.getId(),
                admin.getEmail()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

// DTOs de entrada e saída
record TenantRequest(
        @NotBlank(message = "O nome da empresa é obrigatório.") @Size(max = 100, message = "O nome da empresa deve ter até 100 caracteres.") String companyName,
        @NotBlank(message = "O nome do administrador é obrigatório.") String adminName,
        @NotBlank(message = "O e-mail do administrador é obrigatório.") @Email(message = "O e-mail do administrador é inválido.") String adminEmail,
        @NotBlank(message = "A senha é obrigatória.") @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.") String adminPassword) {}

record TenantResponse(UUID id, String companyName, Boolean active, UUID adminId, String adminEmail) {}

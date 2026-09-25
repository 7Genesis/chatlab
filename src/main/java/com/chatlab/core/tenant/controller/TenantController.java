package com.chatlab.core.tenant.controller;

import com.chatlab.core.tenant.domain.Tenant;
import com.chatlab.core.tenant.service.TenantService;
import com.chatlab.core.user.domain.User;
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
    public ResponseEntity<TenantResponse> create(@RequestBody TenantRequest request) {
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
record TenantRequest(String companyName, String adminName, String adminEmail, String adminPassword) {}

record TenantResponse(UUID id, String companyName, Boolean active, UUID adminId, String adminEmail) {}

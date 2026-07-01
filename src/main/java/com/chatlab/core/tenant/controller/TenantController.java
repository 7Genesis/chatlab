package com.chatlab.core.tenant.controller;

import com.chatlab.core.tenant.domain.Tenant;
import com.chatlab.core.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<Tenant> create(@RequestBody TenantRequest request) {
        Tenant createdTenant = tenantService.createTenantWithAdmin(
                request.companyName(),
                request.adminName(),
                request.adminEmail(),
                request.adminPassword()
        );
        return ResponseEntity.ok(createdTenant);
    }
}

// DTO (Data Transfer Object) para receber os dados do JSON
record TenantRequest(String companyName, String adminName, String adminEmail, String adminPassword) {}
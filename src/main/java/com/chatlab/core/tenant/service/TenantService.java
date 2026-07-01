package com.chatlab.core.tenant.service;

import com.chatlab.core.tenant.domain.Tenant;
import com.chatlab.core.tenant.repository.TenantRepository;
import com.chatlab.core.user.domain.User;
import com.chatlab.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Transactional
    public Tenant createTenantWithAdmin(String companyName, String adminName, String adminEmail, String adminPassword) {
        // Criar empresa

        Tenant tenant = Tenant.builder()
                .companyName(companyName)
                .active(true)
                .build();

        // Criar o admin da empresa
        User adminUser = User.builder()
                .tenant(tenant)
                .name(adminName)
                .email(adminEmail)
                .passwordHash(adminPassword)
                .role("ROLE_ADMIN")
                .active(true)
                .build();

        tenant.setUsers(java.util.List.of(adminUser));

        return tenantRepository.save(tenant);
    }
    
    
}

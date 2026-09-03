package com.np3.ledgerai.web.controller;

import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.web.dto.MeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final CurrentUserProvider currentUserProvider;
    private final TenantContext tenantContext;

    public MeController(CurrentUserProvider currentUserProvider, TenantContext tenantContext) {
        this.currentUserProvider = currentUserProvider;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/api/v1/me")
    public MeResponse me() {
        return new MeResponse(currentUserProvider.currentUserId(), tenantContext.currentTenantId());
    }
}

package com.np3.ledgerai.infrastructure.tenancy;

import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class SingleTenantContext implements TenantContext {
    private static final TenantId THE_ONE_TENANT =
            TenantId.of(UUID.fromString("03ab4406-123e-4491-ae06-5a93a7724555"));

    @Override
    public TenantId currentTenantId() {
        return THE_ONE_TENANT;
    }
}

package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.valueobject.TenantId;

public interface TenantContext {
    TenantId currentTenantId();
}

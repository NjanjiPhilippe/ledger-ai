package com.np3.ledgerai.web.dto;

import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.UserId;

import java.util.List;

/**
 * This is the response that the frontend will get when asking for the current user.
 **/
public record MeResponse(
       UserId userId, TenantId tenantId
) {
}

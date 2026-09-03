package com.np3.ledgerai.infrastructure.security;

import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UserId currentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return UserId.of(UUID.fromString(jwt.getSubject()));
    }
}

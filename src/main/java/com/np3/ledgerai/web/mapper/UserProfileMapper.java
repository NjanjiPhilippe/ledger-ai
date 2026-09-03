package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.infrastructure.persistence.Entity.AppUserProfileEntity;
import com.np3.ledgerai.web.dto.MeResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Assembles the web-facing from the two things that make up "who the caller is".
 */
@Component
public class UserProfileMapper {

    public MeResponse toMeResponse(AppUserProfileEntity profile, Collection<? extends GrantedAuthority> authorities) {
        List<String> authorityNames = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

//        return new MeResponse(
//                profile.getKeycloakSubjectId(),
//                profile.getCompanyId()
//        );
        return null;
    }
}
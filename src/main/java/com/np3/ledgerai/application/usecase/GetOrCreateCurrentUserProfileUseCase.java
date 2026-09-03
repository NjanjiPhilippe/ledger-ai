package com.np3.ledgerai.application.usecase;

import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AppUserProfileEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.AppUserProfileJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Using the to use the design pattern : One class = one use case.
 */
@Component
public class GetOrCreateCurrentUserProfileUseCase {

    private final AppUserProfileJpaRepository appUserProfileJpaRepository;

    public GetOrCreateCurrentUserProfileUseCase(AppUserProfileJpaRepository appUserProfileJpaRepository) {
        this.appUserProfileJpaRepository = appUserProfileJpaRepository;
    }

    public AppUserProfileEntity execute(UserId userId) {
        return appUserProfileJpaRepository.findById(userId.asString())
                .orElseGet(() -> provisionDefaultProfile(userId));
    }

    private AppUserProfileEntity provisionDefaultProfile(UserId userId) {
        AppUserProfileEntity profile = new AppUserProfileEntity();
        profile.setKeycloakSubjectId(userId.asString());
        profile.setCreatedAt(Instant.now());
        // displayName / companyId / roleInCompany intentionally left null:
        // populated later via a dedicated "complete your profile" use case, not invented here.
        return appUserProfileJpaRepository.save(profile);
    }
}

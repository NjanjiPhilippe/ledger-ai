package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.UserProfile;
import com.np3.ledgerai.domain.port.UserProfileRepository;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AppUserProfileEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.AppUserProfileJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserProfileRepositoryAdapter implements UserProfileRepository {

    private final AppUserProfileJpaRepository jpaRepository;

    public JpaUserProfileRepositoryAdapter(AppUserProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<UserProfile> findById(UserId userId) {
        return jpaRepository.findById(userId.asString()).map(JpaUserProfileRepositoryAdapter::toDomain);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        return toDomain(jpaRepository.save(toEntity(profile)));
    }

    private static UserProfile toDomain(AppUserProfileEntity entity) {
        return new UserProfile(
                UserId.of(entity.getKeycloakSubjectId()),
                entity.getDisplayName(),
                entity.getCompanyId(),
                entity.getRoleInCompany(),
                entity.getCreatedAt());
    }

    private static AppUserProfileEntity toEntity(UserProfile profile) {
        return new AppUserProfileEntity(
                profile.userId().asString(),
                profile.displayName(),
                profile.companyId(),
                profile.roleInCompany(),
                profile.createdAt());
    }
}

package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.UserProfile;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AppUserProfileEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.AppUserProfileJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserProfileRepositoryAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private AppUserProfileJpaRepository jpaRepository;

    private JpaUserProfileRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaUserProfileRepositoryAdapter(jpaRepository);
    }

    @Test
    void findByIdMapsTheEntityToTheDomainProfile() {
        UserId userId = UserId.of(UUID.randomUUID());
        when(jpaRepository.findById(userId.asString())).thenReturn(Optional.of(
                new AppUserProfileEntity(userId.asString(), "Ada", "company-1", "CFO", CREATED_AT)));

        Optional<UserProfile> result = adapter.findById(userId);

        assertThat(result).contains(new UserProfile(userId, "Ada", "company-1", "CFO", CREATED_AT));
    }

    @Test
    void findByIdReturnsEmptyWhenNothingIsStored() {
        UserId userId = UserId.of(UUID.randomUUID());
        when(jpaRepository.findById(userId.asString())).thenReturn(Optional.empty());

        assertThat(adapter.findById(userId)).isEmpty();
    }

    @Test
    void savePersistsAnEntityAndReturnsTheDomainProfile() {
        UserId userId = UserId.of(UUID.randomUUID());
        when(jpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile saved = adapter.save(UserProfile.provision(userId, CREATED_AT));

        assertThat(saved).isEqualTo(UserProfile.provision(userId, CREATED_AT));
    }
}

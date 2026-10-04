package com.np3.ledgerai.application.profile.command.useCase;

import com.np3.ledgerai.domain.model.UserProfile;
import com.np3.ledgerai.domain.port.UserProfileRepository;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOrCreateCurrentUserProfileUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private UserProfileRepository repository;

    private GetOrCreateCurrentUserProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetOrCreateCurrentUserProfileUseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void returnsTheExistingProfileWithoutSavingAnything() {
        UserId userId = UserId.of(UUID.randomUUID());
        UserProfile existing = new UserProfile(userId, "Ada", "company-1", "CFO", NOW.minusSeconds(60));
        when(repository.findById(userId)).thenReturn(Optional.of(existing));

        UserProfile result = useCase.execute(userId);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void provisionsADefaultProfileWhenNoneExistsYet() {
        UserId userId = UserId.of(UUID.randomUUID());
        when(repository.findById(userId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = useCase.execute(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.displayName()).isNull();
        verify(repository).save(result);
    }
}

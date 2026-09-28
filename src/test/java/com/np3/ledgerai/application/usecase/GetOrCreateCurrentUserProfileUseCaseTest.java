package com.np3.ledgerai.application.usecase;

import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AppUserProfileEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.AppUserProfileJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * NOTE: I haven't seen AppUserProfileEntity's source, only its usage inside
 * GetOrCreateCurrentUserProfileUseCase (setKeycloakSubjectId, setCreatedAt) and the
 * sibling entities' Lombok pattern (@Getter @Setter @NoArgsConstructor). This test
 * assumes a no-arg constructor plus getKeycloakSubjectId()/getCreatedAt() accessors
 * following that same pattern -- flag if that's not the case.
 */
@ExtendWith(MockitoExtension.class)
class GetOrCreateCurrentUserProfileUseCaseTest {

    @Mock
    private AppUserProfileJpaRepository repository;

    private GetOrCreateCurrentUserProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetOrCreateCurrentUserProfileUseCase(repository);
    }

    @Test
    void returnsTheExistingProfileWhenOneAlreadyExists() {
        UserId userId = UserId.of(UUID.randomUUID());
        AppUserProfileEntity existing = new AppUserProfileEntity();
        existing.setKeycloakSubjectId(userId.asString());
        when(repository.findById(userId.asString())).thenReturn(Optional.of(existing));

        AppUserProfileEntity result = useCase.execute(userId);

        assertThat(result).isSameAs(existing);
        verify(repository, never()).save(any());
    }

    @Test
    void provisionsADefaultProfileWhenNoneExistsYet() {
        UserId userId = UserId.of(UUID.randomUUID());
        when(repository.findById(userId.asString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUserProfileEntity result = useCase.execute(userId);

        assertThat(result.getKeycloakSubjectId()).isEqualTo(userId.asString());
        assertThat(result.getCreatedAt()).isNotNull();

        ArgumentCaptor<AppUserProfileEntity> captor = ArgumentCaptor.forClass(AppUserProfileEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKeycloakSubjectId()).isEqualTo(userId.asString());
    }

    @Test
    void doesNotOverwriteAnExistingProfileWithADefaultOne() {
        UserId userId = UserId.of(UUID.randomUUID());
        AppUserProfileEntity existing = new AppUserProfileEntity();
        existing.setKeycloakSubjectId(userId.asString());
        when(repository.findById(userId.asString())).thenReturn(Optional.of(existing));

        useCase.execute(userId);

        verify(repository, never()).save(any());
    }
}
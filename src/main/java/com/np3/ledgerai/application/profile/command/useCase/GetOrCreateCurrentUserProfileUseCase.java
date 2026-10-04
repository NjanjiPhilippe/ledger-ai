package com.np3.ledgerai.application.profile.command.useCase;

import com.np3.ledgerai.domain.model.UserProfile;
import com.np3.ledgerai.domain.port.UserProfileRepository;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class GetOrCreateCurrentUserProfileUseCase {

    private final UserProfileRepository userProfileRepository;
    private final Clock clock;

    public GetOrCreateCurrentUserProfileUseCase(UserProfileRepository userProfileRepository, Clock clock) {
        this.userProfileRepository = userProfileRepository;
        this.clock = clock;
    }

    @Transactional
    public UserProfile execute(UserId userId) {
        return userProfileRepository.findById(userId)
                .orElseGet(() -> userProfileRepository.save(UserProfile.provision(userId, clock.instant())));
    }
}

package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.model.UserProfile;
import com.np3.ledgerai.domain.valueobject.UserId;

import java.util.Optional;

public interface UserProfileRepository {

    Optional<UserProfile> findById(UserId userId);

    UserProfile save(UserProfile profile);
}

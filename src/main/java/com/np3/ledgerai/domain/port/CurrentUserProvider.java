package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.valueobject.UserId;

public interface CurrentUserProvider {
    UserId currentUserId();
}

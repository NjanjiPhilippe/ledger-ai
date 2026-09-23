package com.np3.ledgerai.domain.valueObject;

import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void ofStringParsesAValidUuid() {
        UUID uuid = UUID.randomUUID();

        UserId userId = UserId.of(uuid.toString());

        assertThat(userId.value()).isEqualTo(uuid);
    }

    @Test
    void ofStringWrapsAnInvalidUuidInAClearerMessage() {
        assertThatThrownBy(() -> UserId.of("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not-a-uuid");
    }

    @Test
    void ofStringRejectsNull() {
        assertThatThrownBy(() -> UserId.of((String) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void ofUuidWrapsTheValueDirectly() {
        UUID uuid = UUID.randomUUID();

        UserId userId = UserId.of(uuid);

        assertThat(userId.value()).isEqualTo(uuid);
    }

    @Test
    void asStringReturnsTheUuidsStringForm() {
        UUID uuid = UUID.randomUUID();

        assertThat(UserId.of(uuid).asString()).isEqualTo(uuid.toString());
    }

    @Test
    void theCompactConstructorRejectsANullValue() {
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
    }
}

package com.np3.ledgerai.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record JournalEntryId(UUID value) {
    public JournalEntryId {
        Objects.requireNonNull(value, "value");
    }

    public static JournalEntryId of(UUID value) {
        return new JournalEntryId(value);
    }

    public static JournalEntryId generate() {
        return new JournalEntryId(UUID.randomUUID());
    }
}

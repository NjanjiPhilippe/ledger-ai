package com.np3.ledgerai.domain.exception;

import com.np3.ledgerai.domain.valueobject.JournalEntryId;

public class JournalEntryNotFoundException extends RuntimeException {
    public JournalEntryNotFoundException(JournalEntryId id) {
        super("Journal entry not found: " + id.value());
    }
}

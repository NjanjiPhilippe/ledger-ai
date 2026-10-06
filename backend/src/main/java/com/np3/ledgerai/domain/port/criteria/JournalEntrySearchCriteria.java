package com.np3.ledgerai.domain.port.criteria;

import com.np3.ledgerai.domain.model.JournalEntryStatus;

import java.time.Instant;

public record JournalEntrySearchCriteria(JournalEntryStatus status, Instant createdFrom, Instant createdTo) {
}

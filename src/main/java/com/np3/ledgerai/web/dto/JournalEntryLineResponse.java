package com.np3.ledgerai.web.dto;

import com.np3.ledgerai.domain.valueobject.EntryType;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineResponse(UUID accountId, BigDecimal amount, EntryType entryType) {
}

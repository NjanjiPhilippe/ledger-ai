package com.np3.ledgerai.web.dto.journalEntry;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.np3.ledgerai.domain.valueobject.EntryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record JournalEntryLineResponse(
        UUID accountId,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(type = "string", pattern = "^-?\\d+(\\.\\d+)?$", description = "Exact decimal amount, serialized as a string")
        BigDecimal amount,
        EntryType entryType) {
}

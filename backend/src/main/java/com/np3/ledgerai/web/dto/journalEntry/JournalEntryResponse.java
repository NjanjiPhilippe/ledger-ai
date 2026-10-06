package com.np3.ledgerai.web.dto.journalEntry;

import com.np3.ledgerai.domain.model.JournalEntryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(
        UUID id,
        String description,
        List<JournalEntryLineResponse> lines,
        JournalEntryStatus status,
        Instant createdAt,
        @Schema(types = {"string", "null"}, description = "Null while the entry is a draft")
        Instant postedAt,
        @Schema(types = {"string", "null"}, description = "Set when this entry reverses another one")
        UUID reversalOfId) {
}

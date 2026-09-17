package com.np3.ledgerai.web.dto;

import com.np3.ledgerai.domain.model.JournalEntryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JournalEntryResponse(UUID id, String description, List<JournalEntryLineResponse> lines,
                                   JournalEntryStatus status, Instant createdAt, Instant postedAt) {
}

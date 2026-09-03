package com.np3.ledgerai.domain.model;

import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;

import java.time.Instant;
import java.util.List;

public record JournalEntryPosted(JournalEntryId journalEntryId, List<TransactionLine> lines, Instant postedAt) {
}

package com.np3.ledgerai.web.dto.journalEntry;

import java.util.List;

public record RecordJournalEntryRequest(String description, String currencyCode,
                                        List<JournalEntryLineRequest> lines) {
}

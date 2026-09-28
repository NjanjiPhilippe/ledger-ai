package com.np3.ledgerai.application.journalEntry.command;

import com.np3.ledgerai.domain.model.JournalEntryPosted;
import com.np3.ledgerai.domain.valueobject.TenantId;

public record JournalEntryPostedEvent(TenantId tenantId, JournalEntryPosted posted) {
}

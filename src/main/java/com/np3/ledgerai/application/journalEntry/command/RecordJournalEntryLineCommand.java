package com.np3.ledgerai.application.journalEntry.command;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;

import java.math.BigDecimal;

public record RecordJournalEntryLineCommand(AccountId accountId, BigDecimal amount, EntryType entryType) {
}

package com.np3.ledgerai.application.journalEntry;

import java.util.Currency;
import java.util.List;

public record RecordJournalEntryCommand(String description, Currency currency,
                                        List<RecordJournalEntryLineCommand> lines){
}

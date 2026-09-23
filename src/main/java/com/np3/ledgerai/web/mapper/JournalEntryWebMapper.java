package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.application.journalEntry.command.RecordJournalEntryCommand;
import com.np3.ledgerai.application.journalEntry.command.RecordJournalEntryLineCommand;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.web.dto.journalEntry.JournalEntryLineResponse;
import com.np3.ledgerai.web.dto.journalEntry.JournalEntryResponse;
import com.np3.ledgerai.web.dto.journalEntry.RecordJournalEntryRequest;

import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

public final class JournalEntryWebMapper {

    private JournalEntryWebMapper() {
    }

    public static RecordJournalEntryCommand toCommand(RecordJournalEntryRequest request) {
        List<RecordJournalEntryLineCommand> lines = request.lines().stream()
                .map(line -> new RecordJournalEntryLineCommand(
                        AccountId.of(line.accountId()),
                        line.amount(),
                        line.entryType()))
                .collect(Collectors.toList());

        return new RecordJournalEntryCommand(
                request.description(),
                Currency.getInstance(request.currencyCode()),
                lines);
    }

    public static JournalEntryResponse toResponse(JournalEntry journalEntry) {
        List<JournalEntryLineResponse> lines = journalEntry.lines().stream()
                .map(JournalEntryWebMapper::toLineResponse)
                .collect(Collectors.toList());

        return new JournalEntryResponse(
                journalEntry.id().value(),
                journalEntry.description(),
                lines,
                journalEntry.status(),
                journalEntry.createdAt(),
                journalEntry.postedAt(),
                journalEntry.reversalOfId() == null ? null : journalEntry.reversalOfId().value());
    }

    private static JournalEntryLineResponse toLineResponse(TransactionLine line) {
        return new JournalEntryLineResponse(
                line.accountId().value(),
                line.amount().amount(),
                line.entryType());
    }
}

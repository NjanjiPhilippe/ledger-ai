package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.application.journalEntry.command.RecordJournalEntryCommand;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.web.dto.journalEntry.JournalEntryLineRequest;
import com.np3.ledgerai.web.dto.journalEntry.JournalEntryResponse;
import com.np3.ledgerai.web.dto.journalEntry.RecordJournalEntryRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryWebMapperTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Test
    void toCommandConvertsLinesAndCurrency() {
        UUID debitAccountId = UUID.randomUUID();
        UUID creditAccountId = UUID.randomUUID();
        RecordJournalEntryRequest request = new RecordJournalEntryRequest(
                "Office supplies", "XAF",
                List.of(
                        new JournalEntryLineRequest(debitAccountId, BigDecimal.valueOf(100), EntryType.DEBIT),
                        new JournalEntryLineRequest(creditAccountId, BigDecimal.valueOf(100), EntryType.CREDIT)));

        RecordJournalEntryCommand command = JournalEntryWebMapper.toCommand(request);

        assertThat(command.description()).isEqualTo("Office supplies");
        assertThat(command.currency()).isEqualTo(XAF);
        assertThat(command.lines()).hasSize(2);
        assertThat(command.lines().getFirst().accountId()).isEqualTo(AccountId.of(debitAccountId));
        assertThat(command.lines().getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(command.lines().getFirst().entryType()).isEqualTo(EntryType.DEBIT);
        assertThat(command.lines().getLast().accountId()).isEqualTo(AccountId.of(creditAccountId));
    }

    @Test
    void toResponseMapsEveryFieldForADraftEntry() {
        JournalEntry entry = JournalEntry.draft(
                TenantId.of(UUID.randomUUID()),
                List.of(
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)),
                "Office supplies",
                Instant.parse("2026-01-01T00:00:00Z"),
                UserId.of(UUID.randomUUID()));

        JournalEntryResponse response = JournalEntryWebMapper.toResponse(entry);

        assertThat(response.id()).isEqualTo(entry.id().value());
        assertThat(response.description()).isEqualTo("Office supplies");
        assertThat(response.status()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(response.createdAt()).isEqualTo(entry.createdAt());
        assertThat(response.postedAt()).isNull();
        assertThat(response.reversalOfId()).isNull();
        assertThat(response.lines()).hasSize(2);
        assertThat(response.lines().getFirst().entryType()).isEqualTo(EntryType.DEBIT);
        assertThat(response.lines().getFirst().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void toResponseExposesTheReversalOfIdWhenPresent() {
        JournalEntryId originalId = JournalEntryId.generate();
        JournalEntry reversal = JournalEntry.draftReversal(
                TenantId.of(UUID.randomUUID()), originalId,
                List.of(
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT),
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT)),
                "Reversal of: Office supplies",
                Instant.parse("2026-01-02T00:00:00Z"),
                UserId.of(UUID.randomUUID()));

        JournalEntryResponse response = JournalEntryWebMapper.toResponse(reversal);

        assertThat(response.reversalOfId()).isEqualTo(originalId.value());
    }
}
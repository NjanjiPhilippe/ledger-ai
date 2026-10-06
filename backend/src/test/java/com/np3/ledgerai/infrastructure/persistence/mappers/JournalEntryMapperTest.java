package com.np3.ledgerai.infrastructure.persistence.mappers;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JournalEntryMapperTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Test
    void toEntityThenToDomainRoundTripsADraftEntry() {
        JournalEntry original = JournalEntry.draft(
                TenantId.of(UUID.randomUUID()),
                List.of(
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)),
                "Office supplies",
                Instant.parse("2026-01-01T00:00:00Z"),
                UserId.of(UUID.randomUUID()));

        JournalEntryEntity entity = JournalEntryMapper.toEntity(original);
        JournalEntry roundTripped = JournalEntryMapper.toDomain(entity);

        assertThat(roundTripped.id()).isEqualTo(original.id());
        assertThat(roundTripped.tenantId()).isEqualTo(original.tenantId());
        assertThat(roundTripped.description()).isEqualTo(original.description());
        assertThat(roundTripped.createdAt()).isEqualTo(original.createdAt());
        assertThat(roundTripped.createdBy()).isEqualTo(original.createdBy());
        assertThat(roundTripped.status()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(roundTripped.postedAt()).isNull();
        assertThat(roundTripped.reversalOfId()).isNull();
        assertThat(roundTripped.lines()).isEqualTo(original.lines());
    }

    @Test
    void toEntityThenToDomainRoundTripsAReversalLink() {
        JournalEntryId originalId = JournalEntryId.generate();
        JournalEntry reversal = JournalEntry.draftReversal(
                TenantId.of(UUID.randomUUID()),
                originalId,
                List.of(
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT),
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT)),
                "Reversal of: Office supplies",
                Instant.parse("2026-01-02T00:00:00Z"),
                UserId.of(UUID.randomUUID()));

        JournalEntryEntity entity = JournalEntryMapper.toEntity(reversal);
        JournalEntry roundTripped = JournalEntryMapper.toDomain(entity);

        assertThat(roundTripped.reversalOfId()).isEqualTo(originalId);
        assertThat(roundTripped.isReversal()).isTrue();
    }

    @Test
    void preservesLineCurrencyThroughTheEmbeddable() {
        JournalEntry original = JournalEntry.draft(
                TenantId.of(UUID.randomUUID()),
                List.of(
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                        new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)),
                "Office supplies",
                Instant.parse("2026-01-01T00:00:00Z"),
                UserId.of(UUID.randomUUID()));

        JournalEntry roundTripped = JournalEntryMapper.toDomain(JournalEntryMapper.toEntity(original));

        assertThat(roundTripped.lines()).allMatch(line -> line.amount().currency().equals(XAF));
    }
}
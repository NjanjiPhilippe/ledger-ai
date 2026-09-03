package com.np3.ledgerai.infrastructure.persistence.mappers;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryLineEmbeddable;

import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

public final class JournalEntryMapper {
    private JournalEntryMapper() {
    }

    public static JournalEntry toDomain(JournalEntryEntity entity) {
        List<TransactionLine> lines = entity.getLines().stream()
                .map(JournalEntryMapper::toDomainLine)
                .collect(Collectors.toList());

        return JournalEntry.reconstitute(
                JournalEntryId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                lines,
                entity.getDescription(),
                entity.getCreatedAt(),
                UserId.of(entity.getCreatedBy()),
                entity.getStatus(),
                entity.getPostedAt());
    }

    public static JournalEntryEntity toEntity(JournalEntry journalEntry) {
        List<JournalEntryLineEmbeddable> lines = journalEntry.lines().stream()
                .map(JournalEntryMapper::toEmbeddableLine)
                .collect(Collectors.toList());

        return new JournalEntryEntity(
                journalEntry.id().value(),
                journalEntry.tenantId().value(),
                lines,
                journalEntry.description(),
                journalEntry.createdAt(),
                journalEntry.createdBy().value(),
                journalEntry.status(),
                journalEntry.postedAt());
    }

    private static TransactionLine toDomainLine(JournalEntryLineEmbeddable line) {
        Currency currency = Currency.getInstance(line.getCurrencyCode());
        Money amount = Money.of(line.getAmount(), currency);
        return new TransactionLine(AccountId.of(line.getAccountId()), amount, line.getEntryType());
    }

    private static JournalEntryLineEmbeddable toEmbeddableLine(TransactionLine line) {
        return new JournalEntryLineEmbeddable(
                line.accountId().value(),
                line.amount().amount(),
                line.amount().currency().getCurrencyCode(),
                line.entryType());
    }
}

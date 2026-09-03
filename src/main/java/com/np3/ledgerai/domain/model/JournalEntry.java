package com.np3.ledgerai.domain.model;

import com.np3.ledgerai.domain.exception.InvalidStateTransitionException;
import com.np3.ledgerai.domain.exception.UnbalancedEntryException;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

public final class JournalEntry {
    private final JournalEntryId id;
    private final TenantId tenantId;
    private final List<TransactionLine> lines;
    private final String description;
    private final Instant createdAt;
    private final UserId createdBy;
    private JournalEntryStatus status;
    private Instant postedAt;

    private JournalEntry(JournalEntryId id, TenantId tenantId, List<TransactionLine> lines, String description,
                         Instant createdAt, UserId createdBy, JournalEntryStatus status, Instant postedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.lines = List.copyOf(lines);
        this.description = description;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.status = status;
        this.postedAt = postedAt;
    }
    public static JournalEntry draft(TenantId tenantId, List<TransactionLine> lines, String description,
                                     Instant createdAt, UserId createdBy) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(createdBy, "createdBy");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Journal entry description must not be blank");
        }
        if (lines.size() < 2) {
            throw new UnbalancedEntryException("A journal entry needs at least two lines");
        }

        Currency currency = lines.get(0).amount().currency();
        for (TransactionLine line : lines) {
            if (!line.amount().currency().equals(currency)) {
                throw new UnbalancedEntryException(
                        "All lines in a journal entry must share the same currency");
            }
        }

        Money totalDebits = lines.stream()
                .filter(TransactionLine::isDebit)
                .map(TransactionLine::amount)
                .reduce(Money.zero(currency), Money::add);
        Money totalCredits = lines.stream()
                .filter(TransactionLine::isCredit)
                .map(TransactionLine::amount)
                .reduce(Money.zero(currency), Money::add);

        if (!totalDebits.equals(totalCredits)) {
            throw new UnbalancedEntryException(
                    "Debits (" + totalDebits + ") must equal credits (" + totalCredits + ")");
        }

        return new JournalEntry(JournalEntryId.generate(), tenantId, lines, description, createdAt, createdBy,
                JournalEntryStatus.DRAFT, null);
    }

    public static JournalEntry reconstitute(JournalEntryId id, TenantId tenantId, List<TransactionLine> lines,
                                            String description, Instant createdAt, UserId createdBy,
                                            JournalEntryStatus status, Instant postedAt) {
        return new JournalEntry(id, tenantId, lines, description, createdAt, createdBy, status, postedAt);
    }

    public JournalEntryPosted post(Instant postedAt) {
        if (this.status != JournalEntryStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Cannot post a journal entry that is not in Draft status (current status: " + this.status + ")");
        }
        this.status = JournalEntryStatus.POSTED;
        this.postedAt = postedAt;
        return new JournalEntryPosted(id, lines, postedAt);
    }

    public JournalEntryId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public List<TransactionLine> lines() {
        return lines;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public UserId createdBy() {
        return createdBy;
    }

    public JournalEntryStatus status() {
        return status;
    }

    public Instant postedAt() {
        return postedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JournalEntry that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

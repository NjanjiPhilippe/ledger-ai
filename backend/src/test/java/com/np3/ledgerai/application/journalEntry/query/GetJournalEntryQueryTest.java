package com.np3.ledgerai.application.journalEntry.query;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetJournalEntryQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private TenantContext tenantContext;

    private GetJournalEntryQuery query;

    @BeforeEach
    void setUp() {
        query = new GetJournalEntryQuery(journalEntryRepository, tenantContext);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void returnsTheEntryWhenFound() {
        JournalEntry entry = sampleEntry();
        when(journalEntryRepository.findById(TENANT_ID, entry.id())).thenReturn(Optional.of(entry));

        Optional<JournalEntry> result = query.execute(entry.id());

        assertThat(result).contains(entry);
    }

    @Test
    void returnsEmptyWhenNotFound() {
        JournalEntryId missingId = JournalEntryId.generate();
        when(journalEntryRepository.findById(TENANT_ID, missingId)).thenReturn(Optional.empty());

        Optional<JournalEntry> result = query.execute(missingId);

        assertThat(result).isEmpty();
    }

    private static JournalEntry sampleEntry() {
        Currency xaf = Currency.getInstance("XAF");
        List<TransactionLine> lines = List.of(
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), xaf), EntryType.DEBIT),
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), xaf), EntryType.CREDIT));
        return JournalEntry.draft(TENANT_ID, lines, "Office supplies", Instant.parse("2026-01-01T00:00:00Z"),
                UserId.of(UUID.randomUUID()));
    }
}
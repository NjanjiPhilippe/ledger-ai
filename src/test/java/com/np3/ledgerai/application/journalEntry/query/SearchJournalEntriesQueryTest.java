package com.np3.ledgerai.application.journalEntry.query;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchJournalEntriesQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private TenantContext tenantContext;

    private SearchJournalEntriesQuery query;

    @BeforeEach
    void setUp() {
        query = new SearchJournalEntriesQuery(journalEntryRepository, tenantContext);
    }

    @Test
    void delegatesToTheRepositoryWithTheCurrentTenant() {
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
        Currency xaf = Currency.getInstance("XAF");
        List<TransactionLine> lines = List.of(
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), xaf), EntryType.DEBIT),
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), xaf), EntryType.CREDIT));
        JournalEntry entry = JournalEntry.draft(TENANT_ID, lines, "Office supplies",
                Instant.parse("2026-01-01T00:00:00Z"), UserId.of(UUID.randomUUID()));

        JournalEntrySearchCriteria criteria = new JournalEntrySearchCriteria(JournalEntryStatus.DRAFT, null, null);
        PageRequest pageRequest = new PageRequest(0, 20);
        PageResult<JournalEntry> expected = new PageResult<>(List.of(entry), 0, 20, 1);
        when(journalEntryRepository.search(TENANT_ID, criteria, pageRequest)).thenReturn(expected);

        PageResult<JournalEntry> result = query.execute(criteria, pageRequest);

        assertThat(result).isEqualTo(expected);
    }
}
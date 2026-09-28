package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import com.np3.ledgerai.infrastructure.persistence.mappers.JournalEntryMapper;
import com.np3.ledgerai.infrastructure.persistence.repository.JournalEntryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaJournalEntryRepositoryAdapterTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private JournalEntryJpaRepository jpaRepository;

    private JpaJournalEntryRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaJournalEntryRepositoryAdapter(jpaRepository);
    }

    @Test
    void saveMapsToEntitySavesAndMapsBack() {
        JournalEntry entry = draftEntry();
        when(jpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JournalEntry saved = adapter.save(entry);

        assertThat(saved.id()).isEqualTo(entry.id());
        assertThat(saved.description()).isEqualTo(entry.description());
    }

    @Test
    void findByIdDelegatesAndMaps() {
        JournalEntryEntity entity = JournalEntryMapper.toEntity(draftEntry());
        when(jpaRepository.findByIdAndTenantId(entity.getId(), TENANT_ID.value())).thenReturn(Optional.of(entity));

        Optional<JournalEntry> result = adapter.findById(TENANT_ID, JournalEntryId.of(entity.getId()));

        assertThat(result).isPresent();
        assertThat(result.get().description()).isEqualTo("Office supplies");
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        JournalEntryId missingId = JournalEntryId.generate();
        when(jpaRepository.findByIdAndTenantId(missingId.value(), TENANT_ID.value())).thenReturn(Optional.empty());

        assertThat(adapter.findById(TENANT_ID, missingId)).isEmpty();
    }

    @Test
    void searchMapsCriteriaToPageableAndWrapsTheResultingPage() {
        JournalEntryEntity entity = JournalEntryMapper.toEntity(draftEntry());
        Page<JournalEntryEntity> page = new PageImpl<>(List.of(entity));
        when(jpaRepository.findAll(any(Specification.class), any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(page);

        var result = adapter.search(TENANT_ID, new JournalEntrySearchCriteria(null, null, null),
                new com.np3.ledgerai.domain.port.criteria.PageRequest(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).description()).isEqualTo("Office supplies");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void sumPostedLinesForAccountAggregatesDebitsAndCreditsSeparately() {
        AccountId accountId = AccountId.generate();
        JournalEntryJpaRepository.EntryTypeTotal debitRow = entryTypeTotal(EntryType.DEBIT, BigDecimal.valueOf(300));
        JournalEntryJpaRepository.EntryTypeTotal creditRow = entryTypeTotal(EntryType.CREDIT, BigDecimal.valueOf(120));
        when(jpaRepository.sumPostedLinesGroupedByEntryType(TENANT_ID.value(), accountId.value()))
                .thenReturn(List.of(debitRow, creditRow));

        DebitCreditTotals totals = adapter.sumPostedLinesForAccount(TENANT_ID, accountId);

        assertThat(totals.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(totals.totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(120));
    }

    @Test
    void sumPostedLinesForAccountDefaultsMissingSideToZero() {
        AccountId accountId = AccountId.generate();
        when(jpaRepository.sumPostedLinesGroupedByEntryType(TENANT_ID.value(), accountId.value()))
                .thenReturn(List.of(entryTypeTotal(EntryType.DEBIT, BigDecimal.valueOf(300))));

        DebitCreditTotals totals = adapter.sumPostedLinesForAccount(TENANT_ID, accountId);

        assertThat(totals.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(totals.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static JournalEntryJpaRepository.EntryTypeTotal entryTypeTotal(EntryType entryType, BigDecimal total) {
        return new JournalEntryJpaRepository.EntryTypeTotal() {
            @Override
            public EntryType getEntryType() {
                return entryType;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }
        };
    }

    private static JournalEntry draftEntry() {
        List<TransactionLine> lines = List.of(
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT));
        return JournalEntry.draft(TENANT_ID, lines, "Office supplies", Instant.parse("2026-01-01T00:00:00Z"),
                UserId.of(UUID.randomUUID()));
    }
}

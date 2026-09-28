package com.np3.ledgerai.infrastructure.projection;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.domain.model.JournalEntryPosted;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceProjectionUpdaterTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private BalanceProjectionJpaRepository repository;

    private BalanceProjectionUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new BalanceProjectionUpdater(repository);
    }

    @Test
    void createsANewProjectionWhenNoneExistsYet() {
        TenantId tenantId = TenantId.of(UUID.randomUUID());
        AccountId accountId = AccountId.generate();
        JournalEntryPostedEvent event = eventFor(tenantId, List.of(
                new TransactionLine(accountId, Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT)));

        var expectedId = new BalanceProjectionEntity.BalanceProjectionId(tenantId.value(), accountId.value());
        when(repository.findById(expectedId)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        updater.onJournalEntryPosted(event);

        ArgumentCaptor<BalanceProjectionEntity> captor = ArgumentCaptor.forClass(BalanceProjectionEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(captor.getValue().getTotalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void accumulatesOntoAnExistingProjection() {
        TenantId tenantId = TenantId.of(UUID.randomUUID());
        AccountId accountId = AccountId.generate();
        JournalEntryPostedEvent event = eventFor(tenantId, List.of(
                new TransactionLine(accountId, Money.of(BigDecimal.valueOf(50), XAF), EntryType.CREDIT)));

        var expectedId = new BalanceProjectionEntity.BalanceProjectionId(tenantId.value(), accountId.value());
        var existing = new BalanceProjectionEntity(tenantId.value(), accountId.value(),
                BigDecimal.valueOf(300), BigDecimal.valueOf(120));
        when(repository.findById(expectedId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        updater.onJournalEntryPosted(event);

        ArgumentCaptor<BalanceProjectionEntity> captor = ArgumentCaptor.forClass(BalanceProjectionEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(300)); // unchanged
        assertThat(captor.getValue().getTotalCredits()).isEqualByComparingTo(BigDecimal.valueOf(170)); // 120 + 50
    }

    @Test
    void updatesOneProjectionPerLineWhenLinesTouchDifferentAccounts() {
        TenantId tenantId = TenantId.of(UUID.randomUUID());
        AccountId debitAccount = AccountId.generate();
        AccountId creditAccount = AccountId.generate();
        JournalEntryPostedEvent event = eventFor(tenantId, List.of(
                new TransactionLine(debitAccount, Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                new TransactionLine(creditAccount, Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)));

        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        updater.onJournalEntryPosted(event);

        verify(repository, times(2)).save(any());
    }

    private static JournalEntryPostedEvent eventFor(TenantId tenantId, List<TransactionLine> lines) {
        JournalEntryPosted posted = new JournalEntryPosted(JournalEntryId.generate(), lines,
                Instant.parse("2026-01-01T00:00:00Z"));
        return new JournalEntryPostedEvent(tenantId, posted);
    }
}
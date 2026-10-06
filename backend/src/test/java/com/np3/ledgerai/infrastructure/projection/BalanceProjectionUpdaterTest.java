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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(repository.findAllForUpdate(eq(tenantId.value()), any())).thenReturn(List.of());

        updater.onJournalEntryPosted(event);

        ArgumentCaptor<BalanceProjectionEntity> captor = ArgumentCaptor.forClass(BalanceProjectionEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAccountId()).isEqualTo(accountId.value());
        assertThat(captor.getValue().getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(captor.getValue().getTotalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void accumulatesOntoAnExistingLockedProjection() {
        TenantId tenantId = TenantId.of(UUID.randomUUID());
        AccountId accountId = AccountId.generate();
        JournalEntryPostedEvent event = eventFor(tenantId, List.of(
                new TransactionLine(accountId, Money.of(BigDecimal.valueOf(50), XAF), EntryType.CREDIT)));
        var existing = new BalanceProjectionEntity(tenantId.value(), accountId.value(),
                BigDecimal.valueOf(300), BigDecimal.valueOf(120));
        when(repository.findAllForUpdate(eq(tenantId.value()), any())).thenReturn(List.of(existing));

        updater.onJournalEntryPosted(event);

        ArgumentCaptor<BalanceProjectionEntity> captor = ArgumentCaptor.forClass(BalanceProjectionEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(300)); // unchanged
        assertThat(captor.getValue().getTotalCredits()).isEqualByComparingTo(BigDecimal.valueOf(170)); // 120 + 50
    }

    @Test
    void locksAllAffectedAccountsInOneQueryAndSavesOneRowPerAccount() {
        TenantId tenantId = TenantId.of(UUID.randomUUID());
        AccountId debitAccount = AccountId.generate();
        AccountId creditAccount = AccountId.generate();
        JournalEntryPostedEvent event = eventFor(tenantId, List.of(
                new TransactionLine(debitAccount, Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                new TransactionLine(creditAccount, Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)));
        when(repository.findAllForUpdate(eq(tenantId.value()), any())).thenReturn(List.of());

        updater.onJournalEntryPosted(event);

        ArgumentCaptor<Collection<UUID>> locked = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findAllForUpdate(eq(tenantId.value()), locked.capture());
        assertThat(locked.getValue()).containsExactlyInAnyOrder(debitAccount.value(), creditAccount.value());
        verify(repository, times(2)).save(any());
    }

    @Test
    void mergesSeveralLinesOnTheSameAccountIntoASingleUpdate() {
        TenantId tenantId = TenantId.of(UUID.randomUUID());
        AccountId cash = AccountId.generate();
        AccountId sales = AccountId.generate();
        JournalEntryPostedEvent event = eventFor(tenantId, List.of(
                new TransactionLine(cash, Money.of(BigDecimal.valueOf(60), XAF), EntryType.DEBIT),
                new TransactionLine(cash, Money.of(BigDecimal.valueOf(40), XAF), EntryType.DEBIT),
                new TransactionLine(sales, Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)));
        when(repository.findAllForUpdate(eq(tenantId.value()), any())).thenReturn(List.of());

        updater.onJournalEntryPosted(event);

        ArgumentCaptor<BalanceProjectionEntity> captor = ArgumentCaptor.forClass(BalanceProjectionEntity.class);
        verify(repository, times(2)).save(captor.capture());
        var cashRow = captor.getAllValues().stream().filter(e -> e.getAccountId().equals(cash.value())).findFirst().orElseThrow();
        assertThat(cashRow.getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    private static JournalEntryPostedEvent eventFor(TenantId tenantId, List<TransactionLine> lines) {
        JournalEntryPosted posted = new JournalEntryPosted(JournalEntryId.generate(), lines,
                Instant.parse("2026-01-01T00:00:00Z"));
        return new JournalEntryPostedEvent(tenantId, posted);
    }
}
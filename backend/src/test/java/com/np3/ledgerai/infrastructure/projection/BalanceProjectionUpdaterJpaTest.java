package com.np3.ledgerai.infrastructure.projection;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.domain.model.JournalEntryPosted;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real updater against a real (embedded) database, with real transactions and real row locks.
 * The mocks in BalanceProjectionUpdaterTest cannot prove any of this.
 */
@DataJpaTest
@Import(BalanceProjectionUpdater.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // each call below manages its own transaction
class BalanceProjectionUpdaterJpaTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Autowired
    private BalanceProjectionUpdater updater;
    @Autowired
    private BalanceProjectionJpaRepository projections;
    @Autowired
    private AccountJpaRepository accounts;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void refusesToRunOutsideATransactionRatherThanSilentlyDoingNothing() {
        TenantId tenant = TenantId.of(UUID.randomUUID());

        assertThatThrownBy(() -> updater.onJournalEntryPosted(debit(tenant, AccountId.generate(), 10)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void concurrentPostingsOnTheSameAccountLoseNoUpdate() throws Exception {
        TenantId tenant = TenantId.of(UUID.randomUUID());
        UUID account = openAccount(tenant);
        // Existing row: this test targets lost updates, not the first-row insert race.
        projections.save(new BalanceProjectionEntity(tenant.value(), account, BigDecimal.ZERO, BigDecimal.ZERO));

        int threads = 8;
        int postingsPerThread = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < postingsPerThread; i++) {
                    tx.executeWithoutResult(status ->
                            updater.onJournalEntryPosted(debit(tenant, AccountId.of(account), 100)));
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        var projection = projections.findById(new BalanceProjectionEntity.BalanceProjectionId(tenant.value(), account))
                .orElseThrow();
        assertThat(projection.getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(threads * postingsPerThread * 100L));
    }

    @Test
    void aFailureAfterTheProjectionUpdateRollsBackTheWholeTransaction() {
        TenantId tenant = TenantId.of(UUID.randomUUID());
        UUID account = openAccount(tenant);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            updater.onJournalEntryPosted(debit(tenant, AccountId.of(account), 100));
            throw new IllegalStateException("something else in the posting transaction failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(projections.findAllByTenantId(tenant.value())).isEmpty();
    }

    @Test
    void createsTheRowOnFirstPostingAndAccumulatesAfterwards() {
        TenantId tenant = TenantId.of(UUID.randomUUID());
        UUID account = openAccount(tenant);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        tx.executeWithoutResult(s -> updater.onJournalEntryPosted(debit(tenant, AccountId.of(account), 100)));
        tx.executeWithoutResult(s -> updater.onJournalEntryPosted(debit(tenant, AccountId.of(account), 50)));

        var rows = projections.findAllByTenantId(tenant.value());
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getTotalDebits()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    private UUID openAccount(TenantId tenant) {
        AccountEntity entity = new AccountEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenant.value());
        entity.setName("Account " + entity.getId());
        entity.setType(com.np3.ledgerai.domain.valueobject.AccountType.ASSET);
        entity.setCurrencyCode("XAF");
        entity.setActive(true);
        return accounts.save(entity).getId();
    }

    private static JournalEntryPostedEvent debit(TenantId tenant, AccountId account, long amount) {
        var line = new TransactionLine(account, Money.of(BigDecimal.valueOf(amount), XAF), EntryType.DEBIT);
        return new JournalEntryPostedEvent(tenant,
                new JournalEntryPosted(JournalEntryId.generate(), List.of(line), Instant.parse("2026-01-01T00:00:00Z")));
    }
}

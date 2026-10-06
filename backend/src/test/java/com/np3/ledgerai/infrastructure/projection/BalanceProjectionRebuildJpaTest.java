package com.np3.ledgerai.infrastructure.projection;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.application.reporting.command.RebuildBalanceProjectionResult;
import com.np3.ledgerai.application.reporting.command.useCase.RebuildBalanceProjectionUseCase;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryPosted;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.adapter.JpaBalanceProjectionRepositoryAdapter;
import com.np3.ledgerai.infrastructure.persistence.adapter.JpaJournalEntryRepositoryAdapter;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole chain on a real database: postings feed the projection through the updater, drift is injected, and
 * the rebuild detects and repairs it from the journal. It also pins down that a reversed entry still counts.
 */
@DataJpaTest
@Import({BalanceProjectionUpdater.class, JpaJournalEntryRepositoryAdapter.class,
        JpaBalanceProjectionRepositoryAdapter.class, RebuildBalanceProjectionUseCase.class,
        BalanceProjectionRebuildJpaTest.FixedTenant.class})
class BalanceProjectionRebuildJpaTest {

    private static final TenantId TENANT = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final UserId USER = UserId.of(UUID.randomUUID());
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @TestConfiguration
    static class FixedTenant {
        @Bean
        TenantContext tenantContext() {
            return () -> TENANT;
        }
    }

    @Autowired
    private BalanceProjectionUpdater updater;
    @Autowired
    private JpaJournalEntryRepositoryAdapter journalEntries;
    @Autowired
    private RebuildBalanceProjectionUseCase rebuild;
    @Autowired
    private BalanceProjectionJpaRepository projections;
    @Autowired
    private AccountJpaRepository accounts;
    @Autowired
    private JpaBalanceProjectionRepositoryAdapter projectionAdapter;
    @Autowired
    private EntityManager entityManager;

    @Test
    void sumPostedLinesByAccountCountsPostedAndReversedEntriesButNotDrafts() {
        AccountId cash = openAccount("Cash", AccountType.ASSET);
        AccountId sales = openAccount("Sales", AccountType.REVENUE);
        postedEntry(cash, sales, 100);
        JournalEntry reversed = postedEntry(cash, sales, 40);
        reversal(reversed);
        journalEntries.save(draft(cash, sales, 999)); // must be ignored

        Map<AccountId, DebitCreditTotals> totals = journalEntries.sumPostedLinesByAccount(TENANT);

        // cash: debits 100 + 40 (the reversed original still counts), credits 40 (its reversal)
        assertThat(totals.get(cash).totalDebits()).isEqualByComparingTo("140");
        assertThat(totals.get(cash).totalCredits()).isEqualByComparingTo("40");
        assertThat(totals.get(sales).totalDebits()).isEqualByComparingTo("40");
        assertThat(totals.get(sales).totalCredits()).isEqualByComparingTo("140");
    }

    @Test
    void rebuildRepairsDriftedMissingAndStaleRowsAndLeavesAnExactProjection() {
        AccountId cash = openAccount("Cash", AccountType.ASSET);
        AccountId sales = openAccount("Sales", AccountType.REVENUE);
        AccountId ghost = openAccount("Ghost", AccountType.EXPENSE);

        // Live operation: every posting feeds the projection through the updater.
        feed(postedEntry(cash, sales, 100));
        JournalEntry reversed = postedEntry(cash, sales, 40);
        feed(reversed);
        feed(reversal(reversed));
        entityManager.flush();

        // Drift: a corrupted row, a missing row, and a row for an account with no activity.
        BalanceProjectionEntity cashRow = projections.findById(
                new BalanceProjectionEntity.BalanceProjectionId(TENANT.value(), cash.value())).orElseThrow();
        cashRow.setTotalDebits(BigDecimal.ONE);
        projections.deleteById(new BalanceProjectionEntity.BalanceProjectionId(TENANT.value(), sales.value()));
        projections.save(new BalanceProjectionEntity(TENANT.value(), ghost.value(), BigDecimal.TEN, BigDecimal.ZERO));
        entityManager.flush();
        entityManager.clear();

        RebuildBalanceProjectionResult result = rebuild.execute();
        entityManager.flush();
        entityManager.clear();

        assertThat(result.accountsRebuilt()).isEqualTo(2);
        assertThat(result.accountsCorrected()).isEqualTo(3); // cash (corrupted), sales (missing), ghost (stale)
        Map<AccountId, DebitCreditTotals> repaired = projectionAdapter.findAllTotals(TENANT);
        assertThat(repaired).containsOnlyKeys(cash, sales);
        assertThat(repaired.get(cash).totalDebits()).isEqualByComparingTo("140");
        assertThat(repaired.get(cash).totalCredits()).isEqualByComparingTo("40");
        assertThat(repaired.get(sales).totalDebits()).isEqualByComparingTo("40");
        assertThat(repaired.get(sales).totalCredits()).isEqualByComparingTo("140");

        // Idempotent: a second rebuild finds nothing to correct.
        assertThat(rebuild.execute().accountsCorrected()).isZero();
    }

    @Test
    void aProjectionFedByTheUpdaterMatchesTheJournalSoRebuildFindsNoDrift() {
        AccountId cash = openAccount("Cash", AccountType.ASSET);
        AccountId sales = openAccount("Sales", AccountType.REVENUE);
        feed(postedEntry(cash, sales, 70));
        JournalEntry reversed = postedEntry(cash, sales, 30);
        feed(reversed);
        feed(reversal(reversed));
        entityManager.flush();
        entityManager.clear();

        assertThat(rebuild.execute().accountsCorrected()).isZero();
    }

    // ---- helpers

    private AccountId openAccount(String name, AccountType type) {
        AccountEntity entity = new AccountEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(TENANT.value());
        entity.setName(name);
        entity.setType(type);
        entity.setCurrencyCode("XAF");
        entity.setActive(true);
        return AccountId.of(accounts.save(entity).getId());
    }

    private JournalEntry draft(AccountId debit, AccountId credit, long amount) {
        return JournalEntry.draft(TENANT, List.of(
                new TransactionLine(debit, Money.of(BigDecimal.valueOf(amount), XAF), EntryType.DEBIT),
                new TransactionLine(credit, Money.of(BigDecimal.valueOf(amount), XAF), EntryType.CREDIT)),
                "entry " + amount, T0, USER);
    }

    private final java.util.Map<JournalEntry, JournalEntryPosted> postedEvents = new java.util.IdentityHashMap<>();

    private JournalEntry postedEntry(AccountId debit, AccountId credit, long amount) {
        JournalEntry entry = draft(debit, credit, amount);
        postedEvents.put(entry, entry.post(T0.plusSeconds(1)));
        return journalEntries.save(entry);
    }

    /** Reverses an entry the way ReverseJournalEntryUseCase does: an offsetting posted entry, original REVERSED. */
    private JournalEntry reversal(JournalEntry original) {
        List<TransactionLine> opposite = original.lines().stream()
                .map(l -> new TransactionLine(l.accountId(), l.amount(),
                        l.isDebit() ? EntryType.CREDIT : EntryType.DEBIT))
                .toList();
        JournalEntry reversal = JournalEntry.draftReversal(TENANT, original.id(), opposite, "reversal", T0, USER);
        postedEvents.put(reversal, reversal.post(T0.plusSeconds(2)));
        original.markReversed();
        journalEntries.save(original);
        return journalEntries.save(reversal);
    }

    private void feed(JournalEntry entry) {
        // Same event the use cases publish; the entity manager is flushed so queries see the rows.
        updater.onJournalEntryPosted(new JournalEntryPostedEvent(TENANT, postedEventOf(entry)));
    }

    private JournalEntryPosted postedEventOf(JournalEntry saved) {
        return postedEvents.entrySet().stream()
                .filter(e -> e.getKey().id().equals(saved.id()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow();
    }
}

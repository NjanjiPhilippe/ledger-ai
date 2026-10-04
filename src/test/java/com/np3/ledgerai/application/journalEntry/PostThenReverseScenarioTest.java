package com.np3.ledgerai.application.journalEntry;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.application.journalEntry.command.useCase.PostJournalEntryUseCase;
import com.np3.ledgerai.application.journalEntry.command.useCase.ReverseJournalEntryUseCase;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.criteria.AccountSearchCriteria;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Framework-free scenario: real use cases wired to an in-memory repository and an
 * in-memory projection fed by the published events. It guards the invariant that
 * the projection must stay in sync with the journal, reversals included.
 */
class PostThenReverseScenarioTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-03T00:00:00Z"), ZoneOffset.UTC);

    private final InMemoryJournalEntryRepository repository = new InMemoryJournalEntryRepository();
    private final Map<AccountId, BigDecimal[]> projection = new HashMap<>(); // [debits, credits]

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();

    private final PostJournalEntryUseCase postUseCase = new PostJournalEntryUseCase(
            repository, accounts, () -> TENANT_ID, CLOCK, event -> applyToProjection((JournalEntryPostedEvent) event));
    private final ReverseJournalEntryUseCase reverseUseCase = new ReverseJournalEntryUseCase(
            repository, () -> TENANT_ID, () -> USER_ID, CLOCK,
            event -> applyToProjection((JournalEntryPostedEvent) event));

    @Test
    void postingThenReversingAnEntryNetsEveryAccountBackToZero() {
        AccountId cash = accounts.open("Cash", AccountType.ASSET);
        AccountId revenue = accounts.open("Sales", AccountType.REVENUE);
        JournalEntry draft = JournalEntry.draft(TENANT_ID, List.of(
                        new TransactionLine(cash, Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                        new TransactionLine(revenue, Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT)),
                "Sale", Instant.parse("2026-01-01T00:00:00Z"), USER_ID);
        repository.save(draft);

        postUseCase.execute(draft.id());
        assertThat(totalsOf(cash).totalDebits()).isEqualByComparingTo("100");
        assertThat(totalsOf(revenue).totalCredits()).isEqualByComparingTo("100");

        reverseUseCase.execute(draft.id());

        // Reversal counted: debits and credits on each account are both 100, so the net balance is 0.
        for (AccountId account : List.of(cash, revenue)) {
            DebitCreditTotals totals = totalsOf(account);
            assertThat(totals.totalDebits()).isEqualByComparingTo("100");
            assertThat(totals.totalCredits()).isEqualByComparingTo("100");
        }

        // The event-fed projection agrees with the journal, which is what a rebuild would produce.
        Map<AccountId, DebitCreditTotals> fromJournal = repository.sumPostedLinesByAccount(TENANT_ID);
        for (AccountId account : List.of(cash, revenue)) {
            assertThat(totalsOf(account).sameAmountsAs(fromJournal.get(account))).isTrue();
        }
    }

    private void applyToProjection(JournalEntryPostedEvent event) {
        for (TransactionLine line : event.posted().lines()) {
            BigDecimal[] totals = projection.computeIfAbsent(line.accountId(),
                    id -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int index = line.isDebit() ? 0 : 1;
            totals[index] = totals[index].add(line.amount().amount());
        }
    }

    private DebitCreditTotals totalsOf(AccountId accountId) {
        BigDecimal[] totals = projection.getOrDefault(accountId, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        return new DebitCreditTotals(totals[0], totals[1]);
    }

    private static final class InMemoryJournalEntryRepository implements JournalEntryRepository {
        private final Map<JournalEntryId, JournalEntry> store = new HashMap<>();

        @Override
        public JournalEntry save(JournalEntry journalEntry) {
            store.put(journalEntry.id(), journalEntry);
            return journalEntry;
        }

        @Override
        public Optional<JournalEntry> findById(TenantId tenantId, JournalEntryId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public PageResult<JournalEntry> search(TenantId tenantId, JournalEntrySearchCriteria criteria,
                                               PageRequest pageRequest) {
            return new PageResult<>(new ArrayList<>(store.values()), 0, pageRequest.size(), store.size());
        }

        @Override
        public DebitCreditTotals sumPostedLinesForAccount(TenantId tenantId, AccountId accountId) {
            return sumPostedLinesByAccount(tenantId).getOrDefault(accountId, DebitCreditTotals.zero());
        }

        @Override
        public Map<AccountId, DebitCreditTotals> sumPostedLinesByAccount(TenantId tenantId) {
            Map<AccountId, BigDecimal[]> sums = new HashMap<>();
            for (JournalEntry entry : store.values()) {
                if (entry.status() == JournalEntryStatus.DRAFT) {
                    continue; // posted entries, including the ones that were later reversed
                }
                for (TransactionLine line : entry.lines()) {
                    BigDecimal[] totals = sums.computeIfAbsent(line.accountId(),
                            id -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                    int index = line.isDebit() ? 0 : 1;
                    totals[index] = totals[index].add(line.amount().amount());
                }
            }
            Map<AccountId, DebitCreditTotals> result = new HashMap<>();
            sums.forEach((id, totals) -> result.put(id, new DebitCreditTotals(totals[0], totals[1])));
            return result;
        }
    }

    private static final class InMemoryAccountRepository implements AccountRepository {
        private final Map<AccountId, Account> store = new HashMap<>();

        AccountId open(String name, AccountType type) {
            Account account = Account.open(TENANT_ID, name, type, XAF);
            store.put(account.id(), account);
            return account.id();
        }

        @Override
        public Account save(Account account) {
            store.put(account.id(), account);
            return account;
        }

        @Override
        public Optional<Account> findById(TenantId tenantId, AccountId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public PageResult<Account> search(TenantId tenantId, AccountSearchCriteria criteria, PageRequest pageRequest) {
            return new PageResult<>(new ArrayList<>(store.values()), 0, pageRequest.size(), store.size());
        }

        @Override
        public boolean existsById(TenantId tenantId, AccountId id) {
            return store.containsKey(id);
        }

        @Override
        public List<Account> findAllByTenant(TenantId tenantId) {
            return new ArrayList<>(store.values());
        }
    }
}

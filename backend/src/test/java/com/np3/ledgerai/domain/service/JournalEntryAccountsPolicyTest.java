package com.np3.ledgerai.domain.service;

import com.np3.ledgerai.domain.exception.InvalidAccountReferenceException;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryAccountsPolicyTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Currency EUR = Currency.getInstance("EUR");

    private final Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
    private final Account sales = Account.open(TENANT_ID, "Sales", AccountType.REVENUE, XAF);

    @Test
    void acceptsActiveAccountsInTheLineCurrency() {
        var accounts = Map.of(cash.id(), cash, sales.id(), sales);

        assertThatCode(() -> JournalEntryAccountsPolicy.requireUsableAccounts(
                lines(XAF), id -> Optional.ofNullable(accounts.get(id)))).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnknownAccount() {
        var accounts = Map.of(cash.id(), cash);

        assertThatThrownBy(() -> JournalEntryAccountsPolicy.requireUsableAccounts(
                lines(XAF), id -> Optional.ofNullable(accounts.get(id))))
                .isInstanceOf(InvalidAccountReferenceException.class)
                .hasMessageContaining(sales.id().value().toString());
    }

    @Test
    void rejectsAnInactiveAccount() {
        sales.deactivate();
        var accounts = Map.of(cash.id(), cash, sales.id(), sales);

        assertThatThrownBy(() -> JournalEntryAccountsPolicy.requireUsableAccounts(
                lines(XAF), id -> Optional.ofNullable(accounts.get(id))))
                .isInstanceOf(InvalidAccountReferenceException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void rejectsACurrencyMismatch() {
        var accounts = Map.of(cash.id(), cash, sales.id(), sales);

        assertThatThrownBy(() -> JournalEntryAccountsPolicy.requireUsableAccounts(
                lines(EUR), id -> Optional.ofNullable(accounts.get(id))))
                .isInstanceOf(InvalidAccountReferenceException.class)
                .hasMessageContaining("XAF").hasMessageContaining("EUR");
    }

    @Test
    void looksEachAccountUpOnlyOnceEvenWhenItAppearsOnSeveralLines() {
        AtomicInteger lookups = new AtomicInteger();
        List<TransactionLine> lines = List.of(
                line(cash, EntryType.DEBIT, 60), line(cash, EntryType.DEBIT, 40), line(sales, EntryType.CREDIT, 100));

        JournalEntryAccountsPolicy.requireUsableAccounts(lines, id -> {
            lookups.incrementAndGet();
            return Optional.of(id.equals(cash.id()) ? cash : sales);
        });

        assertThat(lookups).hasValue(2);
    }

    private List<TransactionLine> lines(Currency currency) {
        return List.of(
                new TransactionLine(cash.id(), Money.of(BigDecimal.valueOf(100), currency), EntryType.DEBIT),
                new TransactionLine(sales.id(), Money.of(BigDecimal.valueOf(100), currency), EntryType.CREDIT));
    }

    private TransactionLine line(Account account, EntryType type, int amount) {
        return new TransactionLine(account.id(), Money.of(BigDecimal.valueOf(amount), XAF), type);
    }
}

package com.np3.ledgerai.domain.service;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountBalanceCalculatorTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"ASSET", "EXPENSE"})
    void debitNormalAccountsSubtractCreditsFromDebits(AccountType type) {
        Account account = account(type);
        DebitCreditTotals totals = new DebitCreditTotals(BigDecimal.valueOf(300), BigDecimal.valueOf(120));

        Money balance = AccountBalanceCalculator.calculate(account, totals);

        assertThat(balance).isEqualTo(Money.of(BigDecimal.valueOf(180), XAF));
    }

    @ParameterizedTest
    @EnumSource(value = AccountType.class, names = {"LIABILITY", "EQUITY", "REVENUE"})
    void creditNormalAccountsSubtractDebitsFromCredits(AccountType type) {
        Account account = account(type);
        DebitCreditTotals totals = new DebitCreditTotals(BigDecimal.valueOf(50), BigDecimal.valueOf(200));

        Money balance = AccountBalanceCalculator.calculate(account, totals);

        assertThat(balance).isEqualTo(Money.of(BigDecimal.valueOf(150), XAF));
    }

    @Test
    void zeroTotalsProduceAZeroBalance() {
        Account account = account(AccountType.ASSET);

        Money balance = AccountBalanceCalculator.calculate(account, DebitCreditTotals.zero());

        assertThat(balance).isEqualTo(Money.zero(XAF));
    }

    @Test
    void debitNormalBalanceCanGoNegativeWhenCreditsExceedDebits() {
        Account account = account(AccountType.ASSET);
        DebitCreditTotals totals = new DebitCreditTotals(BigDecimal.valueOf(50), BigDecimal.valueOf(200));

        Money balance = AccountBalanceCalculator.calculate(account, totals);

        assertThat(balance).isEqualTo(Money.of(BigDecimal.valueOf(-150), XAF));
    }

    private static Account account(AccountType type) {
        return Account.reconstitute(AccountId.generate(), TENANT_ID, "Test account", type, XAF, true);
    }
}
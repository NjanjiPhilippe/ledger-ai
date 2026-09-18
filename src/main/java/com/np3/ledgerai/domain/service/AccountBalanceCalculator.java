package com.np3.ledgerai.domain.service;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;

public final class AccountBalanceCalculator {
    private AccountBalanceCalculator() {
    }

    public static Money calculate(Account account, DebitCreditTotals totals) {
        Money debits = Money.of(totals.totalDebits(), account.currency());
        Money credits = Money.of(totals.totalCredits(), account.currency());

        return isDebitNormal(account.type())
                ? debits.subtract(credits)
                : credits.subtract(debits);
    }

    private static boolean isDebitNormal(AccountType type) {
        return type == AccountType.ASSET || type == AccountType.EXPENSE;
    }
}

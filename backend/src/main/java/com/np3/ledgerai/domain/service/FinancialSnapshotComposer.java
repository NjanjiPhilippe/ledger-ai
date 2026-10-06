package com.np3.ledgerai.domain.service;

import com.np3.ledgerai.domain.valueobject.AccountBalanceLine;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.domain.valueobject.TrialBalance;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pure domain service (same style as AccountBalanceCalculator): no Spring, no I/O.
 * Turns the ledger's TrialBalance into the FinancialSnapshot the AI advisor consumes,
 * so the server — not the caller — is the source of truth for the figures.
 */
public final class FinancialSnapshotComposer {

    private FinancialSnapshotComposer() {
    }

    public static FinancialSnapshot compose(TrialBalance trialBalance) {
        // Accounts with no activity add tokens (cost) but no signal for the advisor.
        List<AccountBalanceLine> balances = trialBalance.lines().stream()
                .filter(line -> hasActivity(line.totalDebits().amount(), line.totalCredits().amount()))
                .map(line -> new AccountBalanceLine(
                        line.accountId(),
                        line.accountName(),
                        line.accountType(),
                        line.balance()))
                .toList();

        return new FinancialSnapshot(
                trialBalance.generatedAt(),
                trialBalance.currency().getCurrencyCode(),
                balances,
                trialBalance.totalDebits(),
                trialBalance.totalCredits());
    }

    private static boolean hasActivity(BigDecimal debits, BigDecimal credits) {
        return debits.signum() != 0 || credits.signum() != 0;
    }
}
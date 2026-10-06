package com.np3.ledgerai.domain.service;

import com.np3.ledgerai.domain.exception.InvalidAccountReferenceException;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Pure domain rule (same style as AccountBalanceCalculator): the accounts an entry touches must exist for
 * the tenant, be active, and use the currency of the line. The lookup is injected so the rule stays free of I/O.
 */
public final class JournalEntryAccountsPolicy {

    private JournalEntryAccountsPolicy() {
    }

    public static void requireUsableAccounts(List<TransactionLine> lines,
                                             Function<AccountId, Optional<Account>> accountLookup) {
        Map<AccountId, Optional<Account>> lookedUp = new HashMap<>();
        for (TransactionLine line : lines) {
            Account account = lookedUp.computeIfAbsent(line.accountId(), accountLookup)
                    .orElseThrow(() -> new InvalidAccountReferenceException(
                            "Account not found: " + line.accountId().value()));
            if (!account.active()) {
                throw new InvalidAccountReferenceException(
                        "Account is inactive: " + account.name() + " (" + line.accountId().value() + ")");
            }
            if (!account.currency().equals(line.amount().currency())) {
                throw new InvalidAccountReferenceException("Account " + account.name() + " uses "
                        + account.currency().getCurrencyCode() + " but the entry line uses "
                        + line.amount().currency().getCurrencyCode());
            }
        }
    }
}

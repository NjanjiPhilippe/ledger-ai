package com.np3.ledgerai.application.reporting.query;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.service.AccountBalanceCalculator;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TrialBalance;
import com.np3.ledgerai.domain.valueobject.TrialBalanceLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;

@Service
public class GetTrialBalanceQuery {

    // Fallback only used when the tenant has zero accounts yet (so there is no
    // account to read a currency from). Adjust to whatever your default/base
    // currency actually is, or wire it from tenant config once that exists.
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("XAF");

    private final AccountRepository accountRepository;
    private final BalanceProjectionRepository balanceProjectionRepository;
    private final TenantContext tenantContext;

    public GetTrialBalanceQuery(AccountRepository accountRepository,
                                BalanceProjectionRepository balanceProjectionRepository,
                                TenantContext tenantContext) {
        this.accountRepository = accountRepository;
        this.balanceProjectionRepository = balanceProjectionRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('VIEWER')")
    public TrialBalance execute() {
        var tenantId = tenantContext.currentTenantId();

        List<Account> accounts = accountRepository.findAllByTenant(tenantId);
        Map<AccountId, DebitCreditTotals> totalsByAccount = balanceProjectionRepository.findAllTotals(tenantId);

        Currency currency = accounts.isEmpty() ? DEFAULT_CURRENCY : accounts.get(0).currency();

        List<TrialBalanceLine> lines = accounts.stream()
                .map(account -> toLine(account, totalsByAccount.getOrDefault(account.id(), DebitCreditTotals.zero())))
                .toList();

        BigDecimal totalDebitsAmount = lines.stream()
                .map(line -> line.totalDebits().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCreditsAmount = lines.stream()
                .map(line -> line.totalCredits().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TrialBalance(
                Instant.now(),
                currency,
                lines,
                Money.of(totalDebitsAmount, currency),
                Money.of(totalCreditsAmount, currency)
        );
    }

    private TrialBalanceLine toLine(Account account, DebitCreditTotals totals) {
        Money debits = Money.of(totals.totalDebits(), account.currency());
        Money credits = Money.of(totals.totalCredits(), account.currency());
        Money balance = AccountBalanceCalculator.calculate(account, totals);

        return new TrialBalanceLine(account.id(), account.name(), account.type(), debits, credits, balance);
    }
}
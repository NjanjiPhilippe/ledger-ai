package com.np3.ledgerai.application.reporting.query;

import com.np3.ledgerai.application.dto.TrialBalance;
import com.np3.ledgerai.application.dto.TrialBalanceLine;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.service.AccountBalanceCalculator;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

@Service
public class GetTrialBalanceQuery {

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
        TenantId tenantId = tenantContext.currentTenantId();
        List<Account> accounts = accountRepository.findAllByTenant(tenantId);

        List<TrialBalanceLine> lines = accounts.stream()
                .map(account -> toLine(tenantId, account))
                .toList();

        Currency currency = accounts.isEmpty() ? null : accounts.getFirst().currency();

        BigDecimal totalDebits = sumWhere(lines, true);
        BigDecimal totalCredits = sumWhere(lines, false);
        boolean balanced = totalDebits.compareTo(totalCredits) == 0;

        return new TrialBalance(lines, totalDebits, totalCredits, currency, balanced);
    }

    private TrialBalanceLine toLine(TenantId tenantId, Account account) {
        DebitCreditTotals totals = balanceProjectionRepository.findTotals(tenantId, account.id())
                .orElse(DebitCreditTotals.zero());
        Money balance = AccountBalanceCalculator.calculate(account, totals);
        return new TrialBalanceLine(account.id(), account.name(), account.type(), balance);
    }

    private static BigDecimal sumWhere(List<TrialBalanceLine> lines, boolean debitColumn) {
        return lines.stream()
                .filter(line -> AccountBalanceCalculator.isDebitNormal(line.accountType()) == debitColumn)
                .map(line -> line.balance().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

package com.np3.ledgerai.application.account;

import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.service.AccountBalanceCalculator;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.Money;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class GetAccountBalanceQuery {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TenantContext tenantContext;

    public GetAccountBalanceQuery(AccountRepository accountRepository,
                                  JournalEntryRepository journalEntryRepository, TenantContext tenantContext) {
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('VIEWER')")
    public Money execute(AccountId accountId) {
        var tenantId = tenantContext.currentTenantId();
        var account = accountRepository.findById(tenantId, accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        var totals = journalEntryRepository.sumPostedLinesForAccount(tenantId, accountId);
        return AccountBalanceCalculator.calculate(account, totals);
    }
}

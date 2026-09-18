package com.np3.ledgerai.application.account;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.port.criteria.AccountSearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SearchAccountsQuery {

    private final AccountRepository accountRepository;
    private final TenantContext tenantContext;

    public SearchAccountsQuery(AccountRepository accountRepository, TenantContext tenantContext) {
        this.accountRepository = accountRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('VIEWER')")
    public PageResult<Account> execute(AccountSearchCriteria criteria, PageRequest pageRequest) {
        return accountRepository.search(tenantContext.currentTenantId(), criteria, pageRequest);
    }
}

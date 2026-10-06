package com.np3.ledgerai.application.account.query;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetAccountQuery {

    private final AccountRepository accountRepository;
    private final TenantContext tenantContext;

    public GetAccountQuery(AccountRepository accountRepository, TenantContext tenantContext) {
        this.accountRepository = accountRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('VIEWER')")
    public Optional<Account> execute(AccountId id) {
        return accountRepository.findById(tenantContext.currentTenantId(), id);
    }
}

package com.np3.ledgerai.application.account;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final TenantContext tenantContext;

    public CreateAccountUseCase(AccountRepository accountRepository, TenantContext tenantContext) {
        this.accountRepository = accountRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public Account execute(CreateAccountCommand command) {
        Account account = Account.open(
                tenantContext.currentTenantId(),
                command.name(),
                command.type(),
                command.currency());

        return accountRepository.save(account);
    }
}

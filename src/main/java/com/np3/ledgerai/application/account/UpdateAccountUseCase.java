package com.np3.ledgerai.application.account;

import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.web.dto.account.UpdateAccountCommand;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class UpdateAccountUseCase {

    private final AccountRepository accountRepository;
    private final TenantContext tenantContext;

    public UpdateAccountUseCase(AccountRepository accountRepository, TenantContext tenantContext) {
        this.accountRepository = accountRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Account execute(UpdateAccountCommand command) {
        Account account = accountRepository.findById(tenantContext.currentTenantId(), command.id())
                .orElseThrow(() -> new AccountNotFoundException(command.id()));

        account.rename(command.name());

        if (command.active() && !account.active()) {
            account.reactivate();
        } else if (!command.active() && account.active()) {
            account.deactivate();
        }

        return accountRepository.save(account);
    }
}

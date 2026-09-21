package com.np3.ledgerai.application.account.command.useCase;

import com.np3.ledgerai.application.account.command.UpdateAccountCommand;
import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAccountUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TenantContext tenantContext;

    private UpdateAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAccountUseCase(accountRepository, tenantContext);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void renamesAndSavesTheAccount() {
        Account account = activeAccount("Cash");
        when(accountRepository.findById(TENANT_ID, account.id())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = useCase.execute(new UpdateAccountCommand(account.id(), "Petty Cash", true));

        assertThat(result.name()).isEqualTo("Petty Cash");
        assertThat(result.active()).isTrue();
        verify(accountRepository).save(account);
    }

    @Test
    void reactivatesAnInactiveAccountWhenActiveIsTrue() {
        Account account = activeAccount("Cash");
        account.deactivate();
        when(accountRepository.findById(TENANT_ID, account.id())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = useCase.execute(new UpdateAccountCommand(account.id(), "Cash", true));

        assertThat(result.active()).isTrue();
    }

    @Test
    void deactivatesAnActiveAccountWhenActiveIsFalse() {
        Account account = activeAccount("Cash");
        when(accountRepository.findById(TENANT_ID, account.id())).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = useCase.execute(new UpdateAccountCommand(account.id(), "Cash", false));

        assertThat(result.active()).isFalse();
    }

    @Test
    void throwsWhenTheAccountDoesNotExist() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(TENANT_ID, missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpdateAccountCommand(missingId, "Cash", true)))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never()).save(any());
    }

    private static Account activeAccount(String name) {
        return Account.open(TENANT_ID, name, AccountType.ASSET, XAF);
    }
}
package com.np3.ledgerai.application.account.command.useCase;

import com.np3.ledgerai.application.account.command.CreateAccountCommand;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TenantContext tenantContext;

    private CreateAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAccountUseCase(accountRepository, tenantContext);
    }

    @Test
    void opensAndSavesTheAccount() {
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateAccountCommand command = new CreateAccountCommand("Cash", AccountType.ASSET, XAF);

        Account result = useCase.execute(command);

        assertThat(result.tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.name()).isEqualTo("Cash");
        assertThat(result.type()).isEqualTo(AccountType.ASSET);
        assertThat(result.currency()).isEqualTo(XAF);
        assertThat(result.active()).isTrue();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(result);
    }
}
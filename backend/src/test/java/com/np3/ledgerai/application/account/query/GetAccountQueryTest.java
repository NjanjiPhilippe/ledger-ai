package com.np3.ledgerai.application.account.query;

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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAccountQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TenantContext tenantContext;

    private GetAccountQuery query;

    @BeforeEach
    void setUp() {
        query = new GetAccountQuery(accountRepository, tenantContext);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void returnsTheAccountWhenFound() {
        Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, Currency.getInstance("XAF"));
        when(accountRepository.findById(TENANT_ID, account.id())).thenReturn(Optional.of(account));

        Optional<Account> result = query.execute(account.id());

        assertThat(result).contains(account);
    }

    @Test
    void returnsEmptyWhenNotFound() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(TENANT_ID, missingId)).thenReturn(Optional.empty());

        Optional<Account> result = query.execute(missingId);

        assertThat(result).isEmpty();
    }
}
package com.np3.ledgerai.application.account.query;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.port.criteria.AccountSearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchAccountsQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TenantContext tenantContext;

    private SearchAccountsQuery query;

    @BeforeEach
    void setUp() {
        query = new SearchAccountsQuery(accountRepository, tenantContext);
    }

    @Test
    void delegatesToTheRepositoryWithTheCurrentTenant() {
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
        Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, Currency.getInstance("XAF"));
        AccountSearchCriteria criteria = new AccountSearchCriteria(AccountType.ASSET, true, "cas");
        PageRequest pageRequest = new PageRequest(0, 20);
        PageResult<Account> expected = new PageResult<>(List.of(account), 0, 20, 1);
        when(accountRepository.search(TENANT_ID, criteria, pageRequest)).thenReturn(expected);

        PageResult<Account> result = query.execute(criteria, pageRequest);

        assertThat(result).isEqualTo(expected);
    }
}
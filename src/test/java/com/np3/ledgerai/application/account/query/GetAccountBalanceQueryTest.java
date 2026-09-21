package com.np3.ledgerai.application.account.query;

import com.np3.ledgerai.domain.exception.AccountNotFoundException;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class GetAccountBalanceQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BalanceProjectionRepository balanceProjectionRepository;
    @Mock
    private TenantContext tenantContext;

    private GetAccountBalanceQuery query;

    @BeforeEach
    void setUp() {
        query = new GetAccountBalanceQuery(accountRepository, balanceProjectionRepository, tenantContext);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void calculatesTheBalanceFromTheProjection() {
        Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(accountRepository.findById(TENANT_ID, account.id())).thenReturn(Optional.of(account));
        when(balanceProjectionRepository.findTotals(TENANT_ID, account.id()))
                .thenReturn(Optional.of(new DebitCreditTotals(BigDecimal.valueOf(300), BigDecimal.valueOf(100))));

        Money balance = query.execute(account.id());

        assertThat(balance).isEqualTo(Money.of(BigDecimal.valueOf(200), XAF));
    }

    @Test
    void treatsAMissingProjectionAsZero() {
        Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(accountRepository.findById(TENANT_ID, account.id())).thenReturn(Optional.of(account));
        when(balanceProjectionRepository.findTotals(TENANT_ID, account.id())).thenReturn(Optional.empty());

        Money balance = query.execute(account.id());

        assertThat(balance).isEqualTo(Money.zero(XAF));
    }

    @Test
    void throwsWhenTheAccountDoesNotExistAndNeverQueriesTheProjection() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(TENANT_ID, missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> query.execute(missingId))
                .isInstanceOf(AccountNotFoundException.class);

        verify(balanceProjectionRepository, never()).findTotals(any(), any());
    }
}
package com.np3.ledgerai.application.reporting.query;

import com.np3.ledgerai.application.dto.TrialBalance;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTrialBalanceQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private BalanceProjectionRepository balanceProjectionRepository;
    @Mock
    private TenantContext tenantContext;

    private GetTrialBalanceQuery query;

    @BeforeEach
    void setUp() {
        query = new GetTrialBalanceQuery(accountRepository, balanceProjectionRepository, tenantContext);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void balancesWhenDebitAndCreditNormalAccountsNetOut() {
        Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        Account payable = Account.open(TENANT_ID, "Accounts Payable", AccountType.LIABILITY, XAF);
        Account revenue = Account.open(TENANT_ID, "Sales Revenue", AccountType.REVENUE, XAF);
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of(cash, payable, revenue));

        when(balanceProjectionRepository.findTotals(TENANT_ID, cash.id()))
                .thenReturn(Optional.of(new DebitCreditTotals(BigDecimal.valueOf(300), BigDecimal.valueOf(100))));
        when(balanceProjectionRepository.findTotals(TENANT_ID, payable.id()))
                .thenReturn(Optional.of(new DebitCreditTotals(BigDecimal.valueOf(50), BigDecimal.valueOf(250))));
        // no projection yet for revenue -- should default to zero, not blow up
        when(balanceProjectionRepository.findTotals(TENANT_ID, revenue.id())).thenReturn(Optional.empty());

        TrialBalance result = query.execute();

        assertThat(result.lines()).hasSize(3);
        assertThat(result.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(200)); // Cash only
        assertThat(result.totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(200)); // Payable + zero Revenue
        assertThat(result.currency()).isEqualTo(XAF);
        assertThat(result.balanced()).isTrue();
    }

    @Test
    void eachLineCarriesTheAccountsCalculatedBalance() {
        Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of(cash));
        when(balanceProjectionRepository.findTotals(TENANT_ID, cash.id()))
                .thenReturn(Optional.of(new DebitCreditTotals(BigDecimal.valueOf(300), BigDecimal.valueOf(120))));

        TrialBalance result = query.execute();

        assertThat(result.lines()).hasSize(1);
        var line = result.lines().getFirst();
        assertThat(line.accountId()).isEqualTo(cash.id());
        assertThat(line.accountName()).isEqualTo("Cash");
        assertThat(line.accountType()).isEqualTo(AccountType.ASSET);
        assertThat(line.balance().amount()).isEqualByComparingTo(BigDecimal.valueOf(180));
    }

    @Test
    void returnsAnEmptyBalancedReportWhenTheTenantHasNoAccountsYet() {
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of());

        TrialBalance result = query.execute();

        assertThat(result.lines()).isEmpty();
        assertThat(result.totalDebits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.currency()).isNull();
        assertThat(result.balanced()).isTrue();
    }

    @Test
    void flagsAnUnbalancedReportIfTheInvariantEverBroke() {
        // Deliberately inconsistent projections to exercise the balanced-flag logic itself --
        // this shouldn't happen given JournalEntry's own balance invariant, but the report
        // should still surface it accurately rather than assume it away.
        Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of(cash));
        when(balanceProjectionRepository.findTotals(TENANT_ID, cash.id()))
                .thenReturn(Optional.of(new DebitCreditTotals(BigDecimal.valueOf(100), BigDecimal.ZERO)));

        TrialBalance result = query.execute();

        assertThat(result.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.balanced()).isFalse();
    }
}

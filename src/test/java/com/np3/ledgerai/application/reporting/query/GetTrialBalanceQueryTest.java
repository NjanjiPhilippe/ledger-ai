package com.np3.ledgerai.application.reporting.query;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TrialBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTrialBalanceQueryTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-09-29T10:00:00Z");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private Clock clock;
    @Mock
    private BalanceProjectionRepository balanceProjectionRepository;
    @Mock
    private TenantContext tenantContext;

    private GetTrialBalanceQuery query;

    @BeforeEach
    void setUp() {
        query = new GetTrialBalanceQuery(accountRepository, balanceProjectionRepository, tenantContext, clock);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
        when(clock.instant()).thenReturn(FIXED_INSTANT);
    }

    @Test
    void balancesWhenDebitAndCreditNormalAccountsNetOut() {
        Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        Account payable = Account.open(TENANT_ID, "Accounts Payable", AccountType.LIABILITY, XAF);
        Account revenue = Account.open(TENANT_ID, "Sales Revenue", AccountType.REVENUE, XAF);
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of(cash, payable, revenue));

        // no projection row yet for revenue -- getOrDefault(..., DebitCreditTotals.zero()) should cover it
        when(balanceProjectionRepository.findAllTotals(TENANT_ID)).thenReturn(Map.of(
                cash.id(), new DebitCreditTotals(BigDecimal.valueOf(300), BigDecimal.valueOf(100)),
                payable.id(), new DebitCreditTotals(BigDecimal.valueOf(50), BigDecimal.valueOf(250))
        ));

        TrialBalance result = query.execute();

        assertThat(result.lines()).hasSize(3);
        // Raw activity totals across every account (300+50+0 debits, 100+250+0 credits),
        // not the accounting-style "balance placed in its normal column" total.
        assertThat(result.totalDebits().amount()).isEqualByComparingTo(BigDecimal.valueOf(350));
        assertThat(result.totalCredits().amount()).isEqualByComparingTo(BigDecimal.valueOf(350));
        assertThat(result.currency()).isEqualTo(XAF);
        assertThat(result.balanced()).isTrue();
    }

    @Test
    void eachLineCarriesTheAccountsCalculatedBalance() {
        Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of(cash));
        when(balanceProjectionRepository.findAllTotals(TENANT_ID)).thenReturn(Map.of(
                cash.id(), new DebitCreditTotals(BigDecimal.valueOf(300), BigDecimal.valueOf(120))
        ));

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
        when(balanceProjectionRepository.findAllTotals(TENANT_ID)).thenReturn(Map.of());

        TrialBalance result = query.execute();

        assertThat(result.lines()).isEmpty();
        assertThat(result.totalDebits().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalCredits().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        // TrialBalance.currency is non-null by domain invariant, so an accountless
        // tenant falls back to GetTrialBalanceQuery's DEFAULT_CURRENCY (XAF), not null.
        assertThat(result.currency()).isEqualTo(XAF);
        assertThat(result.balanced()).isTrue();
    }

    @Test
    void flagsAnUnbalancedReportIfTheInvariantEverBroke() {
        // Deliberately inconsistent projection to exercise the balanced-flag logic itself --
        // this shouldn't happen given JournalEntry's own balance invariant, but the report
        // should still surface it accurately rather than assume it away.
        Account cash = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(accountRepository.findAllByTenant(TENANT_ID)).thenReturn(List.of(cash));
        when(balanceProjectionRepository.findAllTotals(TENANT_ID)).thenReturn(Map.of(
                cash.id(), new DebitCreditTotals(BigDecimal.valueOf(100), BigDecimal.ZERO)
        ));

        TrialBalance result = query.execute();

        assertThat(result.totalDebits().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result.totalCredits().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.balanced()).isFalse();
    }
}
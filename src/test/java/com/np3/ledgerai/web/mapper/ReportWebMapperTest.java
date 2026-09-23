package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.application.dto.TrialBalance;
import com.np3.ledgerai.application.dto.TrialBalanceLine;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWebMapperTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Test
    void toResponseMapsLinesAndTotals() {
        AccountId cashId = AccountId.generate();
        TrialBalanceLine line = new TrialBalanceLine(cashId, "Cash", AccountType.ASSET,
                Money.of(BigDecimal.valueOf(200), XAF));
        TrialBalance trialBalance = new TrialBalance(List.of(line), BigDecimal.valueOf(200),
                BigDecimal.valueOf(200), XAF, true);

        TrialBalanceResponse response = ReportWebMapper.toResponse(trialBalance);

        assertThat(response.lines()).hasSize(1);
        assertThat(response.lines().get(0).accountId()).isEqualTo(cashId.value());
        assertThat(response.lines().get(0).accountName()).isEqualTo("Cash");
        assertThat(response.lines().get(0).accountType()).isEqualTo(AccountType.ASSET);
        assertThat(response.lines().get(0).balance()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.currencyCode()).isEqualTo("XAF");
        assertThat(response.balanced()).isTrue();
    }

    @Test
    void toResponseHandlesANullCurrencyWhenThereAreNoAccounts() {
        TrialBalance trialBalance = new TrialBalance(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, null, true);

        TrialBalanceResponse response = ReportWebMapper.toResponse(trialBalance);

        assertThat(response.lines()).isEmpty();
        assertThat(response.currencyCode()).isNull();
    }
}
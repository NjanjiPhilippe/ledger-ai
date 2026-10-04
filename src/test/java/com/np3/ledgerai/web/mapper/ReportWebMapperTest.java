package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TrialBalance;
import com.np3.ledgerai.domain.valueobject.TrialBalanceLine;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportWebMapperTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant GENERATED_AT = Instant.parse("2026-09-29T10:00:00Z");

    @Test
    void toResponseMapsLinesAndTotals() {
        AccountId cashId = AccountId.generate();

        // Line-level totals (300/100/200) are deliberately different from the trial
        // balance's own totals (200/200) below, so the test proves the mapper forwards
        // each independently rather than accidentally mixing the two up.
        TrialBalanceLine line = new TrialBalanceLine(cashId, "Cash", AccountType.ASSET,
                Money.of(BigDecimal.valueOf(300), XAF),
                Money.of(BigDecimal.valueOf(100), XAF),
                Money.of(BigDecimal.valueOf(200), XAF));

        TrialBalance trialBalance = new TrialBalance(GENERATED_AT, XAF, List.of(line),
                Money.of(BigDecimal.valueOf(200), XAF),
                Money.of(BigDecimal.valueOf(200), XAF));

        TrialBalanceResponse response = ReportWebMapper.toResponse(trialBalance);

        assertThat(response.generatedAt()).isEqualTo(GENERATED_AT);
        assertThat(response.lines()).hasSize(1);

        var lineResponse = response.lines().get(0);
        assertThat(lineResponse.accountId()).isEqualTo(cashId.value());
        assertThat(lineResponse.accountName()).isEqualTo("Cash");
        assertThat(lineResponse.accountType()).isEqualTo("ASSET");
        assertThat(lineResponse.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(lineResponse.totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(lineResponse.balance()).isEqualByComparingTo(BigDecimal.valueOf(200));

        assertThat(response.totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.currencyCode()).isEqualTo("XAF");
        assertThat(response.balanced()).isTrue();
    }

    @Test
    void toResponseMapsAnEmptyTrialBalance() {
        // TrialBalance.currency is non-null by domain invariant (Objects.requireNonNull
        // in its compact constructor), so there is no "null currency" input to guard
        // against at this layer -- GetTrialBalanceQuery is what decides the fallback
        // currency for an accountless tenant, not this mapper.
        TrialBalance trialBalance = new TrialBalance(GENERATED_AT, XAF, List.of(),
                Money.of(BigDecimal.ZERO, XAF),
                Money.of(BigDecimal.ZERO, XAF));

        TrialBalanceResponse response = ReportWebMapper.toResponse(trialBalance);

        assertThat(response.lines()).isEmpty();
        assertThat(response.currencyCode()).isEqualTo("XAF");
        assertThat(response.totalDebits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.balanced()).isTrue();
    }
}
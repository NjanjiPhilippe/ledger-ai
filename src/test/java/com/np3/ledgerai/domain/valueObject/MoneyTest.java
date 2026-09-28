package com.np3.ledgerai.domain.valueObject;

import com.np3.ledgerai.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency XAF = Currency.getInstance("XAF"); // 0 fraction digits
    private static final Currency USD = Currency.getInstance("USD"); // 2 fraction digits

    @Test
    void ofNormalizesTheScaleToTheCurrencysFractionDigits() {
        Money money = Money.of(BigDecimal.valueOf(100), USD);

        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    void ofRejectsAnAmountWithMoreDecimalsThanTheCurrencyAllows() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("100.50"), XAF))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void ofRequiresNonNullAmountAndCurrency() {
        assertThatThrownBy(() -> Money.of(null, USD)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void zeroProducesAZeroAmountInTheGivenCurrency() {
        Money zero = Money.zero(USD);

        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.currency()).isEqualTo(USD);
        assertThat(zero.isNegative()).isFalse();
    }

    @Test
    void addSumsAmountsInTheSameCurrency() {
        Money result = Money.of(BigDecimal.valueOf(100), XAF).add(Money.of(BigDecimal.valueOf(50), XAF));

        assertThat(result).isEqualTo(Money.of(BigDecimal.valueOf(150), XAF));
    }

    @Test
    void addRejectsDifferentCurrencies() {
        assertThatThrownBy(() ->
                Money.of(BigDecimal.valueOf(100), XAF).add(Money.of(BigDecimal.valueOf(50), USD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subtractSubtractsAmountsInTheSameCurrencyAndCanGoNegative() {
        Money result = Money.of(BigDecimal.valueOf(100), XAF).subtract(Money.of(BigDecimal.valueOf(150), XAF));

        assertThat(result).isEqualTo(Money.of(BigDecimal.valueOf(-50), XAF));
        assertThat(result.isNegative()).isTrue();
    }

    @Test
    void subtractRejectsDifferentCurrencies() {
        assertThatThrownBy(() ->
                Money.of(BigDecimal.valueOf(100), XAF).subtract(Money.of(BigDecimal.valueOf(50), USD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCodeAreBasedOnAmountAndCurrency() {
        Money a = Money.of(BigDecimal.valueOf(100), XAF);
        Money b = Money.of(BigDecimal.valueOf(100), XAF);
        Money differentCurrency = Money.of(BigDecimal.valueOf(100), USD);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(differentCurrency);
    }

    @Test
    void toStringIncludesTheAmountAndCurrencyCode() {
        Money money = Money.of(BigDecimal.valueOf(100), XAF);

        assertThat(money.toString()).contains("100").contains("XAF");
    }
}
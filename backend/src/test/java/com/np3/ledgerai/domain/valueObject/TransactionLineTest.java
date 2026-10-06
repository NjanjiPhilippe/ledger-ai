package com.np3.ledgerai.domain.valueObject;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionLineTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Test
    void isDebitAndIsCreditReflectTheEntryType() {
        TransactionLine debit = new TransactionLine(AccountId.generate(), Money.of(BigDecimal.TEN, XAF), EntryType.DEBIT);
        TransactionLine credit = new TransactionLine(AccountId.generate(), Money.of(BigDecimal.TEN, XAF), EntryType.CREDIT);

        assertThat(debit.isDebit()).isTrue();
        assertThat(debit.isCredit()).isFalse();
        assertThat(credit.isCredit()).isTrue();
        assertThat(credit.isDebit()).isFalse();
    }

    @Test
    void rejectsAZeroAmount() {
        assertThatThrownBy(() ->
                new TransactionLine(AccountId.generate(), Money.zero(XAF), EntryType.DEBIT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsANegativeAmount() {
        Money negative = Money.of(BigDecimal.valueOf(-10), XAF);

        assertThatThrownBy(() -> new TransactionLine(AccountId.generate(), negative, EntryType.DEBIT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresNonNullFields() {
        assertThatThrownBy(() -> new TransactionLine(null, Money.of(BigDecimal.TEN, XAF), EntryType.DEBIT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TransactionLine(AccountId.generate(), null, EntryType.DEBIT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TransactionLine(AccountId.generate(), Money.of(BigDecimal.TEN, XAF), null))
                .isInstanceOf(NullPointerException.class);
    }
}

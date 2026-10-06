package com.np3.ledgerai.domain.model;


import com.np3.ledgerai.domain.exception.InvalidStateTransitionException;
import com.np3.ledgerai.domain.exception.UnbalancedEntryException;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalEntryTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static TransactionLine debit(BigDecimal amount) {
        return new TransactionLine(AccountId.generate(), Money.of(amount, XAF), EntryType.DEBIT);
    }

    private static TransactionLine credit(BigDecimal amount) {
        return new TransactionLine(AccountId.generate(), Money.of(amount, XAF), EntryType.CREDIT);
    }

    private static List<TransactionLine> balancedLines() {
        return List.of(debit(BigDecimal.valueOf(100)), credit(BigDecimal.valueOf(100)));
    }

    @Nested
    @DisplayName("draft()")
    class Draft {

        @Test
        @DisplayName("creates a DRAFT entry when debits equal credits")
        void createsBalancedDraft() {
            JournalEntry entry = JournalEntry.draft(TENANT_ID, balancedLines(), "Office supplies", NOW, USER_ID);

            assertThat(entry.status()).isEqualTo(JournalEntryStatus.DRAFT);
            assertThat(entry.lines()).hasSize(2);
            assertThat(entry.description()).isEqualTo("Office supplies");
            assertThat(entry.isReversal()).isFalse();
            assertThat(entry.postedAt()).isNull();
        }

        @Test
        @DisplayName("rejects fewer than two lines")
        void rejectsSingleLine() {
            List<TransactionLine> lines = List.of(debit(BigDecimal.valueOf(100)));

            assertThatThrownBy(() -> JournalEntry.draft(TENANT_ID, lines, "Invalid", NOW, USER_ID))
                    .isInstanceOf(UnbalancedEntryException.class);
        }

        @Test
        @DisplayName("rejects lines with mismatched currencies")
        void rejectsMismatchedCurrencies() {
            TransactionLine usdLine = new TransactionLine(AccountId.generate(),
                    Money.of(BigDecimal.valueOf(100), Currency.getInstance("USD")), EntryType.CREDIT);
            List<TransactionLine> lines = List.of(debit(BigDecimal.valueOf(100)), usdLine);

            assertThatThrownBy(() -> JournalEntry.draft(TENANT_ID, lines, "Invalid", NOW, USER_ID))
                    .isInstanceOf(UnbalancedEntryException.class)
                    .hasMessageContaining("same currency");
        }

        @Test
        @DisplayName("rejects unbalanced debits and credits")
        void rejectsUnbalancedTotals() {
            List<TransactionLine> lines = List.of(debit(BigDecimal.valueOf(100)), credit(BigDecimal.valueOf(50)));

            assertThatThrownBy(() -> JournalEntry.draft(TENANT_ID, lines, "Invalid", NOW, USER_ID))
                    .isInstanceOf(UnbalancedEntryException.class)
                    .hasMessageContaining("must equal");
        }

        @Test
        @DisplayName("rejects a blank description")
        void rejectsBlankDescription() {
            assertThatThrownBy(() -> JournalEntry.draft(TENANT_ID, balancedLines(), "   ", NOW, USER_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("draftReversal()")
    class DraftReversal {

        @Test
        @DisplayName("requires a non-null reversalOfId")
        void requiresReversalOfId() {
            assertThatThrownBy(() ->
                    JournalEntry.draftReversal(TENANT_ID, null, balancedLines(), "Reversal", NOW, USER_ID))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("marks the entry as a reversal of the original")
        void marksAsReversal() {
            JournalEntryId originalId = JournalEntryId.generate();

            JournalEntry reversal = JournalEntry.draftReversal(
                    TENANT_ID, originalId, balancedLines(), "Reversal", NOW, USER_ID);

            assertThat(reversal.isReversal()).isTrue();
            assertThat(reversal.reversalOfId()).isEqualTo(originalId);
        }
    }

    @Nested
    @DisplayName("post()")
    class Post {

        @Test
        @DisplayName("transitions DRAFT to POSTED and returns a domain event")
        void postsADraftEntry() {
            JournalEntry entry = JournalEntry.draft(TENANT_ID, balancedLines(), "Office supplies", NOW, USER_ID);
            Instant postedAt = NOW.plusSeconds(60);

            JournalEntryPosted event = entry.post(postedAt);

            assertThat(entry.status()).isEqualTo(JournalEntryStatus.POSTED);
            assertThat(entry.postedAt()).isEqualTo(postedAt);
            assertThat(event.journalEntryId()).isEqualTo(entry.id());
            assertThat(event.lines()).isEqualTo(entry.lines());
            assertThat(event.postedAt()).isEqualTo(postedAt);
        }

        @Test
        @DisplayName("cannot post an entry that is already POSTED")
        void cannotRepostAnEntry() {
            JournalEntry entry = JournalEntry.draft(TENANT_ID, balancedLines(), "Office supplies", NOW, USER_ID);
            entry.post(NOW);

            assertThatThrownBy(() -> entry.post(NOW))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("markReversed()")
    class MarkReversed {

        @Test
        @DisplayName("transitions POSTED to REVERSED")
        void marksAPostedEntryAsReversed() {
            JournalEntry entry = JournalEntry.draft(TENANT_ID, balancedLines(), "Office supplies", NOW, USER_ID);
            entry.post(NOW);

            entry.markReversed();

            assertThat(entry.status()).isEqualTo(JournalEntryStatus.REVERSED);
        }

        @Test
        @DisplayName("cannot reverse a DRAFT entry")
        void cannotReverseADraftEntry() {
            JournalEntry entry = JournalEntry.draft(TENANT_ID, balancedLines(), "Office supplies", NOW, USER_ID);

            assertThatThrownBy(entry::markReversed)
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Test
    @DisplayName("equals/hashCode are based on identity only")
    void equalityIsIdentityBased() {
        List<TransactionLine> lines = balancedLines();
        JournalEntry entry = JournalEntry.draft(TENANT_ID, lines, "Office supplies", NOW, USER_ID);
        JournalEntry reconstitutedWithDifferentState = JournalEntry.reconstitute(
                entry.id(), TENANT_ID, lines, "Different description",
                NOW, USER_ID, null, JournalEntryStatus.POSTED, NOW);

        assertThat(entry).isEqualTo(reconstitutedWithDifferentState);
        assertThat(entry).hasSameHashCodeAs(reconstitutedWithDifferentState);
    }
}

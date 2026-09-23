package com.np3.ledgerai.application.journalEntry.command.useCase;

import com.np3.ledgerai.domain.exception.InvalidStateTransitionException;
import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReverseJournalEntryUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant REVERSED_AT = Instant.parse("2026-01-03T00:00:00Z");

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final Clock clock = Clock.fixed(REVERSED_AT, ZoneOffset.UTC);

    private ReverseJournalEntryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReverseJournalEntryUseCase(journalEntryRepository, tenantContext, currentUserProvider, clock);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void reversesAPostedEntryWithOppositeEntryTypes() {
        JournalEntry posted = postedEntry();
        when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findById(TENANT_ID, posted.id())).thenReturn(Optional.of(posted));
        when(journalEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JournalEntry reversal = useCase.execute(posted.id());

        assertThat(reversal.isReversal()).isTrue();
        assertThat(reversal.reversalOfId()).isEqualTo(posted.id());
        assertThat(reversal.status()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(reversal.description()).isEqualTo("Reversal of: Office supplies");
        assertThat(reversal.lines()).extracting(TransactionLine::entryType)
                .containsExactly(EntryType.CREDIT, EntryType.DEBIT); // opposite of the original's DEBIT, CREDIT

        assertThat(posted.status()).isEqualTo(JournalEntryStatus.REVERSED);

        ArgumentCaptor<JournalEntry> savedCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository, times(2)).save(savedCaptor.capture());
        assertThat(savedCaptor.getAllValues()).containsExactly(reversal, posted);
    }

    @Test
    void throwsWhenTheOriginalDoesNotExist() {
        JournalEntryId missingId = JournalEntryId.generate();
        when(journalEntryRepository.findById(TENANT_ID, missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(missingId))
                .isInstanceOf(JournalEntryNotFoundException.class);

        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void cannotReverseAnEntryThatIsNotPosted() {
        JournalEntry draft = draftEntry();
        when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.findById(TENANT_ID, draft.id())).thenReturn(Optional.of(draft));
        when(journalEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> useCase.execute(draft.id()))
                .isInstanceOf(InvalidStateTransitionException.class);

        // Current implementation persists the offsetting reversal BEFORE calling
        // original.markReversed() -- so on a non-posted original, an orphaned
        // reversal entry has already been saved once by the time this throws.
        // Worth deciding whether that's acceptable or whether markReversed()
        // should be validated before the reversal is persisted.
        verify(journalEntryRepository, times(1)).save(any());
    }

    private static JournalEntry postedEntry() {
        JournalEntry entry = draftEntry();
        entry.post(CREATED_AT.plusSeconds(1));
        return entry;
    }

    private static JournalEntry draftEntry() {
        List<TransactionLine> lines = List.of(
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT));
        return JournalEntry.draft(TENANT_ID, lines, "Office supplies", CREATED_AT, USER_ID);
    }
}
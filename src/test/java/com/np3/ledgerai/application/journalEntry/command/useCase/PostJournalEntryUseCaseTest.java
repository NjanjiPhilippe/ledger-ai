package com.np3.ledgerai.application.journalEntry.command.useCase;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.domain.exception.InvalidStateTransitionException;
import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryStatus;
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
import org.springframework.context.ApplicationEventPublisher;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostJournalEntryUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant POSTED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final Clock clock = Clock.fixed(POSTED_AT, ZoneOffset.UTC);

    private PostJournalEntryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PostJournalEntryUseCase(journalEntryRepository, tenantContext, clock, eventPublisher);
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void postsADraftEntryAndPublishesAnEvent() {
        JournalEntry draft = draftEntry();
        when(journalEntryRepository.findById(TENANT_ID, draft.id())).thenReturn(Optional.of(draft));
        when(journalEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        JournalEntry result = useCase.execute(draft.id());

        assertThat(result.status()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(result.postedAt()).isEqualTo(POSTED_AT);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(JournalEntryPostedEvent.class);
    }

    @Test
    void throwsWhenTheEntryDoesNotExist() {
        JournalEntryId missingId = JournalEntryId.generate();
        when(journalEntryRepository.findById(TENANT_ID, missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(missingId))
                .isInstanceOf(JournalEntryNotFoundException.class);

        verify(journalEntryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cannotPostAnAlreadyPostedEntry() {
        JournalEntry alreadyPosted = draftEntry();
        alreadyPosted.post(CREATED_AT.plusSeconds(1));
        when(journalEntryRepository.findById(TENANT_ID, alreadyPosted.id())).thenReturn(Optional.of(alreadyPosted));

        assertThatThrownBy(() -> useCase.execute(alreadyPosted.id()))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(journalEntryRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    private static JournalEntry draftEntry() {
        List<TransactionLine> lines = List.of(
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.DEBIT),
                new TransactionLine(AccountId.generate(), Money.of(BigDecimal.valueOf(100), XAF), EntryType.CREDIT));
        return JournalEntry.draft(TENANT_ID, lines, "Office supplies", CREATED_AT, USER_ID);
    }
}
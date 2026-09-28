package com.np3.ledgerai.application.journalEntry.command.useCase;

import com.np3.ledgerai.application.journalEntry.command.RecordJournalEntryCommand;
import com.np3.ledgerai.application.journalEntry.command.RecordJournalEntryLineCommand;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.TenantId;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordJournalEntryUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final UserId USER_ID = UserId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private TenantContext tenantContext;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    private RecordJournalEntryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RecordJournalEntryUseCase(journalEntryRepository, tenantContext, currentUserProvider, clock);
    }

    @Test
    void recordsABalancedJournalEntryAndDelegatesToRepository() {
        when(tenantContext.currentTenantId()).thenReturn(TENANT_ID);
        when(currentUserProvider.currentUserId()).thenReturn(USER_ID);
        when(journalEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RecordJournalEntryCommand command = new RecordJournalEntryCommand(
                "Office supplies", XAF,
                List.of(
                        new RecordJournalEntryLineCommand(AccountId.generate(), BigDecimal.valueOf(100), EntryType.DEBIT),
                        new RecordJournalEntryLineCommand(AccountId.generate(), BigDecimal.valueOf(100), EntryType.CREDIT)));

        JournalEntry result = useCase.execute(command);

        assertThat(result.description()).isEqualTo("Office supplies");
        assertThat(result.tenantId()).isEqualTo(TENANT_ID);
        assertThat(result.createdBy()).isEqualTo(USER_ID);
        assertThat(result.createdAt()).isEqualTo(FIXED_NOW);
        assertThat(result.lines()).hasSize(2);
        assertThat(result.lines()).allMatch(line -> line.amount().currency().equals(XAF));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(result);
    }
}
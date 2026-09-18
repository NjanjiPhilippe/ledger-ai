package com.np3.ledgerai.application.journalEntry;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.CurrentUserProvider;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordJournalEntryUseCase {

    private final JournalEntryRepository journalEntryRepository;
    private final TenantContext tenantContext;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public RecordJournalEntryUseCase(JournalEntryRepository journalEntryRepository,
                                     TenantContext tenantContext,
                                     CurrentUserProvider currentUserProvider,
                                     Clock clock) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @PreAuthorize("hasRole('ACCOUNTANT') or hasRole('ADMIN')")
    @Transactional
    public JournalEntry execute(RecordJournalEntryCommand command) {
        List<TransactionLine> lines = command.lines().stream()
                .map(line -> new TransactionLine(
                        line.accountId(),
                        Money.of(line.amount(), command.currency()),
                        line.entryType()))
                .collect(Collectors.toList());

        JournalEntry journalEntry = JournalEntry.draft(
                tenantContext.currentTenantId(),
                lines,
                command.description(),
                Instant.now(clock),
                currentUserProvider.currentUserId());

        return journalEntryRepository.save(journalEntry);
    }
}

package com.np3.ledgerai.application.journalEntry;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TransactionLine;
import com.np3.ledgerai.domain.valueobject.UserId;
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
    private final Clock clock;

    public RecordJournalEntryUseCase(JournalEntryRepository journalEntryRepository,
                                     TenantContext tenantContext, Clock clock) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
        this.clock = clock;
    }

    @Transactional
    public JournalEntry execute(RecordJournalEntryCommand command, UserId createdBy) {
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
                createdBy);

        return journalEntryRepository.save(journalEntry);
    }
}

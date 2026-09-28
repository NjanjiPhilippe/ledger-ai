package com.np3.ledgerai.application.journalEntry.command.useCase;

import com.np3.ledgerai.application.journalEntry.command.JournalEntryPostedEvent;
import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.model.JournalEntryPosted;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PostJournalEntryUseCase {

    private final JournalEntryRepository journalEntryRepository;
    private final TenantContext tenantContext;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public PostJournalEntryUseCase(JournalEntryRepository journalEntryRepository, TenantContext tenantContext,
                                   Clock clock, ApplicationEventPublisher eventPublisher) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Transactional
    public JournalEntry execute(JournalEntryId id) {
        JournalEntry journalEntry = journalEntryRepository.findById(tenantContext.currentTenantId(), id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        JournalEntryPosted event = journalEntry.post(Instant.now(clock));
        JournalEntry saved = journalEntryRepository.save(journalEntry);

        eventPublisher.publishEvent(new JournalEntryPostedEvent(saved.tenantId(), event));

        return saved;
    }
}

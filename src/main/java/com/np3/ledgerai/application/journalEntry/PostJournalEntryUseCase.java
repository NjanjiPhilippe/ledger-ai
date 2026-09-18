package com.np3.ledgerai.application.journalEntry;

import com.np3.ledgerai.domain.exception.JournalEntryNotFoundException;
import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
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

    public PostJournalEntryUseCase(JournalEntryRepository journalEntryRepository, TenantContext tenantContext,
                                   Clock clock) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
        this.clock = clock;
    }
    @PreAuthorize("hasRole('ACCOUNTANT')")
    @Transactional
    public JournalEntry execute(JournalEntryId id) {
        JournalEntry journalEntry = journalEntryRepository.findById(tenantContext.currentTenantId(), id)
                .orElseThrow(() -> new JournalEntryNotFoundException(id));

        journalEntry.post(Instant.now(clock));

        return journalEntryRepository.save(journalEntry);
    }
}

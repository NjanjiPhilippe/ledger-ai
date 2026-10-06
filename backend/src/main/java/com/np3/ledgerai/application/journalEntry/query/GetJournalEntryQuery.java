package com.np3.ledgerai.application.journalEntry.query;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetJournalEntryQuery {

    private final JournalEntryRepository journalEntryRepository;
    private final TenantContext tenantContext;

    public GetJournalEntryQuery(JournalEntryRepository journalEntryRepository, TenantContext tenantContext) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('VIEWER')")
    public Optional<JournalEntry> execute(JournalEntryId id) {
        return journalEntryRepository.findById(tenantContext.currentTenantId(), id);
    }
}

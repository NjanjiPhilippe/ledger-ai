package com.np3.ledgerai.application.journalEntry;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class SearchJournalEntriesQuery {

    private final JournalEntryRepository journalEntryRepository;
    private final TenantContext tenantContext;

    public SearchJournalEntriesQuery(JournalEntryRepository journalEntryRepository, TenantContext tenantContext) {
        this.journalEntryRepository = journalEntryRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('VIEWER')")
    public PageResult<JournalEntry> execute(JournalEntrySearchCriteria criteria, PageRequest pageRequest) {
        return journalEntryRepository.search(tenantContext.currentTenantId(), criteria, pageRequest);
    }
}

package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository {

    JournalEntry save(JournalEntry journalEntry);

    Optional<JournalEntry> findById(TenantId tenantId, JournalEntryId id);

    List<JournalEntry> findAllByTenant(TenantId tenantId);
}

package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.Optional;

public interface JournalEntryRepository {

    JournalEntry save(JournalEntry journalEntry);

    Optional<JournalEntry> findById(TenantId tenantId, JournalEntryId id);

    PageResult<JournalEntry> search(TenantId tenantId, JournalEntrySearchCriteria criteria, PageRequest pageRequest);

    DebitCreditTotals sumPostedLinesForAccount(TenantId tenantId, AccountId accountId);
}

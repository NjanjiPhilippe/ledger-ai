package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.Map;
import java.util.Optional;

public interface JournalEntryRepository {

    JournalEntry save(JournalEntry journalEntry);

    Optional<JournalEntry> findById(TenantId tenantId, JournalEntryId id);

    PageResult<JournalEntry> search(TenantId tenantId, JournalEntrySearchCriteria criteria, PageRequest pageRequest);

    /** Totals of the lines that were ever posted (posted or later reversed) for one account. */
    DebitCreditTotals sumPostedLinesForAccount(TenantId tenantId, AccountId accountId);

    /**
     * The source of truth for the balance projection: totals of every line that was ever posted, per account.
     * A reversed entry is included, since its offsetting reversal entry is posted as well.
     */
    Map<AccountId, DebitCreditTotals> sumPostedLinesByAccount(TenantId tenantId);
}

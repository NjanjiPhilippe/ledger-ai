package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.Map;
import java.util.Optional;

public interface BalanceProjectionRepository {
    Optional<DebitCreditTotals> findTotals(TenantId tenantId, AccountId accountId);
    Map<AccountId, DebitCreditTotals> findAllTotals(TenantId tenantId);

    /**
     * Same as {@link #findAllTotals} but locks the tenant's projection rows until the end of the current
     * transaction, so postings wait instead of racing with a rebuild.
     */
    Map<AccountId, DebitCreditTotals> lockAndFindAllTotals(TenantId tenantId);

    /** Makes the tenant's projection exactly equal to {@code totals}: updates, inserts and removes rows. */
    void replaceAll(TenantId tenantId, Map<AccountId, DebitCreditTotals> totals);
}

package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.Optional;

public interface BalanceProjectionRepository {
    Optional<DebitCreditTotals> findTotals(TenantId tenantId, AccountId accountId);
}

package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface BalanceProjectionRepository {
    Optional<DebitCreditTotals> findTotals(TenantId tenantId, AccountId accountId);
    Map<AccountId, DebitCreditTotals> findAllTotals(TenantId tenantId);
}

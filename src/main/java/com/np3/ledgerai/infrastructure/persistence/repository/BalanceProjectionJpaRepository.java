package com.np3.ledgerai.infrastructure.persistence.repository;

import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceProjectionJpaRepository
        extends JpaRepository<BalanceProjectionEntity, BalanceProjectionEntity.BalanceProjectionId> {
    List<BalanceProjectionEntity> findAllByTenantId(UUID tenantId);

    /** SELECT ... FOR UPDATE on every projection row of the tenant. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from BalanceProjectionEntity p where p.tenantId = :tenantId order by p.accountId")
    List<BalanceProjectionEntity> findAllByTenantIdForUpdate(@Param("tenantId") UUID tenantId);

    /**
     * SELECT ... FOR UPDATE on the given accounts, ordered by account id so that concurrent transactions
     * always acquire row locks in the same order (no deadlock between postings touching the same accounts).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from BalanceProjectionEntity p
            where p.tenantId = :tenantId and p.accountId in :accountIds
            order by p.accountId
            """)
    List<BalanceProjectionEntity> findAllForUpdate(@Param("tenantId") UUID tenantId,
                                                   @Param("accountIds") Collection<UUID> accountIds);
}

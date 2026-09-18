package com.np3.ledgerai.infrastructure.persistence.repository;

import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID>,
        JpaSpecificationExecutor<JournalEntryEntity> {

    Optional<JournalEntryEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
        SELECT l.entryType AS entryType, COALESCE(SUM(l.amount), 0) AS total
        FROM JournalEntryEntity e JOIN e.lines l
        WHERE e.tenantId = :tenantId
          AND l.accountId = :accountId
          AND e.status = com.np3.ledgerai.domain.model.JournalEntryStatus.POSTED
        GROUP BY l.entryType
        """)
    List<EntryTypeTotal> sumPostedLinesGroupedByEntryType(@Param("tenantId") UUID tenantId,
                                                          @Param("accountId") UUID accountId);

    interface EntryTypeTotal {
        EntryType getEntryType();
        BigDecimal getTotal();
    }
}

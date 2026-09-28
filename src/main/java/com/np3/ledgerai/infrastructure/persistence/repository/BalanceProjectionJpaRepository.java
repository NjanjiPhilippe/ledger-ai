package com.np3.ledgerai.infrastructure.persistence.repository;

import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BalanceProjectionJpaRepository
        extends JpaRepository<BalanceProjectionEntity, BalanceProjectionEntity.BalanceProjectionId> {
    List<BalanceProjectionEntity> findAllByTenantId(UUID tenantId);

}

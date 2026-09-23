package com.np3.ledgerai.infrastructure.persistence.repository;

import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceProjectionJpaRepository
        extends JpaRepository<BalanceProjectionEntity, BalanceProjectionEntity.BalanceProjectionId> {
}

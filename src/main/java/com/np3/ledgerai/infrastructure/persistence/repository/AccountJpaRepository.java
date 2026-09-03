package com.np3.ledgerai.infrastructure.persistence.repository;

import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<AccountEntity> findAllByTenantId(UUID tenantId);

    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}

package com.np3.ledgerai.infrastructure.persistence.repository;

import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    Optional<JournalEntryEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<JournalEntryEntity> findAllByTenantId(UUID tenantId);
}

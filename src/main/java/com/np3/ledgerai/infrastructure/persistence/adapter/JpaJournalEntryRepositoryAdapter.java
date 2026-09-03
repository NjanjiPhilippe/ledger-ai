package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.JournalEntryJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.mappers.JournalEntryMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaJournalEntryRepositoryAdapter implements JournalEntryRepository {

    private final JournalEntryJpaRepository jpaRepository;

    public JpaJournalEntryRepositoryAdapter(JournalEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public JournalEntry save(JournalEntry journalEntry) {
        JournalEntryEntity saved = jpaRepository.save(JournalEntryMapper.toEntity(journalEntry));
        return JournalEntryMapper.toDomain(saved);
    }

    @Override
    public Optional<JournalEntry> findById(TenantId tenantId, JournalEntryId id) {
        return jpaRepository.findByIdAndTenantId(id.value(), tenantId.value())
                .map(JournalEntryMapper::toDomain);
    }

    @Override
    public List<JournalEntry> findAllByTenant(TenantId tenantId) {
        return jpaRepository.findAllByTenantId(tenantId.value()).stream()
                .map(JournalEntryMapper::toDomain)
                .toList();
    }
}

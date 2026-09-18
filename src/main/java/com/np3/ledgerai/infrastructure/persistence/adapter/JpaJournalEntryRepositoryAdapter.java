package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.JournalEntry;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.criteria.JournalEntrySearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.domain.valueobject.JournalEntryId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.JournalEntryJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.mappers.JournalEntryMapper;
import com.np3.ledgerai.infrastructure.persistence.repository.specifications.JournalEntrySpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    public PageResult<JournalEntry> search(TenantId tenantId, JournalEntrySearchCriteria criteria,
                                           PageRequest pageRequest) {
        Specification<JournalEntryEntity> spec = Specification
                .where(JournalEntrySpecifications.hasTenant(tenantId.value()))
                .and(JournalEntrySpecifications.hasStatus(criteria.status()))
                .and(JournalEntrySpecifications.createdFrom(criteria.createdFrom()))
                .and(JournalEntrySpecifications.createdTo(criteria.createdTo()));

        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());

        Page<JournalEntryEntity> page = jpaRepository.findAll(spec, springPageRequest);

        return new PageResult<>(
                page.getContent().stream().map(JournalEntryMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    public DebitCreditTotals sumPostedLinesForAccount(TenantId tenantId, AccountId accountId) {
        List<JournalEntryJpaRepository.EntryTypeTotal> rows =
                jpaRepository.sumPostedLinesGroupedByEntryType(tenantId.value(), accountId.value());

        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        for (var row : rows) {
            if (row.getEntryType() == EntryType.DEBIT) {
                debits = row.getTotal();
            } else {
                credits = row.getTotal();
            }
        }
        return new DebitCreditTotals(debits, credits);
    }
}

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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaJournalEntryRepositoryAdapter implements JournalEntryRepository {

    // Newest first; the id breaks ties so that page boundaries are stable.
    static final Sort DEFAULT_SORT = Sort.by("createdAt").descending().and(Sort.by("id").descending());

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
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size(), DEFAULT_SORT);

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

    @Override
    public Map<AccountId, DebitCreditTotals> sumPostedLinesByAccount(TenantId tenantId) {
        Map<UUID, BigDecimal[]> byAccount = new HashMap<>(); // [debits, credits]
        for (var row : jpaRepository.sumPostedLinesGroupedByAccountAndEntryType(tenantId.value())) {
            BigDecimal[] totals = byAccount.computeIfAbsent(row.getAccountId(),
                    id -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            totals[row.getEntryType() == EntryType.DEBIT ? 0 : 1] = row.getTotal();
        }
        Map<AccountId, DebitCreditTotals> result = new HashMap<>();
        byAccount.forEach((accountId, totals) ->
                result.put(AccountId.of(accountId), new DebitCreditTotals(totals[0], totals[1])));
        return result;
    }
}

package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JpaBalanceProjectionRepositoryAdapter implements BalanceProjectionRepository {

    private final BalanceProjectionJpaRepository jpaRepository;

    public JpaBalanceProjectionRepositoryAdapter(BalanceProjectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<DebitCreditTotals> findTotals(TenantId tenantId, AccountId accountId) {
        var id = new BalanceProjectionEntity.BalanceProjectionId(tenantId.value(), accountId.value());
        return jpaRepository.findById(id)
                .map(e -> new DebitCreditTotals(e.getTotalDebits(), e.getTotalCredits()));
    }

    @Override
    public Map<AccountId, DebitCreditTotals> findAllTotals(TenantId tenantId) {
        return toTotals(jpaRepository.findAllByTenantId(tenantId.value()));
    }

    @Override
    public Map<AccountId, DebitCreditTotals> lockAndFindAllTotals(TenantId tenantId) {
        return toTotals(jpaRepository.findAllByTenantIdForUpdate(tenantId.value()));
    }

    @Override
    public void replaceAll(TenantId tenantId, Map<AccountId, DebitCreditTotals> totals) {
        // Existing rows are updated in place (and obsolete ones removed) rather than deleted and re-inserted:
        // Hibernate flushes inserts before deletes, which would collide on the primary key.
        List<BalanceProjectionEntity> existing = jpaRepository.findAllByTenantId(tenantId.value());
        Set<UUID> seen = new HashSet<>();
        List<BalanceProjectionEntity> obsolete = new ArrayList<>();
        for (BalanceProjectionEntity row : existing) {
            DebitCreditTotals target = totals.get(AccountId.of(row.getAccountId()));
            if (target == null) {
                obsolete.add(row);
            } else {
                row.setTotalDebits(target.totalDebits());
                row.setTotalCredits(target.totalCredits());
                seen.add(row.getAccountId());
            }
        }
        jpaRepository.deleteAll(obsolete);
        List<BalanceProjectionEntity> created = new ArrayList<>();
        totals.forEach((accountId, target) -> {
            if (!seen.contains(accountId.value())) {
                created.add(new BalanceProjectionEntity(tenantId.value(), accountId.value(),
                        target.totalDebits(), target.totalCredits()));
            }
        });
        jpaRepository.saveAll(created);
    }

    private static Map<AccountId, DebitCreditTotals> toTotals(List<BalanceProjectionEntity> rows) {
        return rows.stream().collect(Collectors.toMap(
                e -> AccountId.of(e.getAccountId()),
                e -> new DebitCreditTotals(e.getTotalDebits(), e.getTotalCredits())));
    }
}

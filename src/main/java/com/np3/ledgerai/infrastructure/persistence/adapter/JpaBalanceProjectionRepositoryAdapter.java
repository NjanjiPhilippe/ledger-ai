package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
}

package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.BalanceProjectionEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.BalanceProjectionJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaBalanceProjectionRepositoryAdapterTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Mock
    private BalanceProjectionJpaRepository jpaRepository;

    private JpaBalanceProjectionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaBalanceProjectionRepositoryAdapter(jpaRepository);
    }

    @Test
    void findTotalsMapsAnExistingProjection() {
        AccountId accountId = AccountId.generate();
        var id = new BalanceProjectionEntity.BalanceProjectionId(TENANT_ID.value(), accountId.value());
        var entity = new BalanceProjectionEntity(TENANT_ID.value(), accountId.value(),
                BigDecimal.valueOf(300), BigDecimal.valueOf(120));
        when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<DebitCreditTotals> result = adapter.findTotals(TENANT_ID, accountId);

        assertThat(result).isPresent();
        assertThat(result.get().totalDebits()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(result.get().totalCredits()).isEqualByComparingTo(BigDecimal.valueOf(120));
    }

    @Test
    void findTotalsReturnsEmptyWhenNoProjectionExistsYet() {
        AccountId accountId = AccountId.generate();
        var id = new BalanceProjectionEntity.BalanceProjectionId(TENANT_ID.value(), accountId.value());
        when(jpaRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(adapter.findTotals(TENANT_ID, accountId)).isEmpty();
    }
}
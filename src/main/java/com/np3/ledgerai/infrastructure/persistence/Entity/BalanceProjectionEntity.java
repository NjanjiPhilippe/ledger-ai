package com.np3.ledgerai.infrastructure.persistence.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "balance_projection")
@IdClass(BalanceProjectionEntity.BalanceProjectionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BalanceProjectionEntity {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "total_debits", nullable = false)
    private BigDecimal totalDebits;

    @Column(name = "total_credits", nullable = false)
    private BigDecimal totalCredits;

    public static class BalanceProjectionId implements Serializable {
        private UUID tenantId;
        private UUID accountId;

        public BalanceProjectionId() {
        }

        public BalanceProjectionId(UUID tenantId, UUID accountId) {
            this.tenantId = tenantId;
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BalanceProjectionId that)) return false;
            return tenantId.equals(that.tenantId) && accountId.equals(that.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, accountId);
        }
    }
}
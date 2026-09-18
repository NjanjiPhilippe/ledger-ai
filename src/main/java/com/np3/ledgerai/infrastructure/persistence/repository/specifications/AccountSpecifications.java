package com.np3.ledgerai.infrastructure.persistence.repository.specifications;

import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class AccountSpecifications {

    private AccountSpecifications() {
    }

    public static Specification<AccountEntity> hasTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<AccountEntity> hasType(AccountType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<AccountEntity> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    public static Specification<AccountEntity> nameContains(String fragment) {
        return (root, query, cb) -> (fragment == null || fragment.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + fragment.toLowerCase() + "%");
    }
}

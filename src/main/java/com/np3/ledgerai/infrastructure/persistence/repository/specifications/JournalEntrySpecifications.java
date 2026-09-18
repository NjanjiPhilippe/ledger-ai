package com.np3.ledgerai.infrastructure.persistence.repository.specifications;

import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class JournalEntrySpecifications {

    private JournalEntrySpecifications() {
    }

    public static Specification<JournalEntryEntity> hasTenant(UUID tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<JournalEntryEntity> hasStatus(JournalEntryStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<JournalEntryEntity> createdFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<JournalEntryEntity> createdTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}

package com.np3.ledgerai.infrastructure.persistence.repository.specifications;

import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.util.List;
import java.util.UUID;

import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.AccountSpecifications.hasTenant;
import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.AccountSpecifications.hasType;
import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.AccountSpecifications.isActive;
import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.AccountSpecifications.nameContains;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses @DataJpaTest's default embedded database + Hibernate schema generation
 * (not your Liquibase changelogs). If you want schema-parity testing, this would
 * need @AutoConfigureTestDatabase(replace = NONE) against a real/Testcontainers
 * Postgres instead.
 */
@DataJpaTest
class AccountSpecificationsTest {

    @Autowired
    private AccountJpaRepository repository;

    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();

        repository.save(account(tenantA, "Cash", AccountType.ASSET, true));
        repository.save(account(tenantA, "Accounts Payable", AccountType.LIABILITY, true));
        repository.save(account(tenantA, "Old Suspense Account", AccountType.ASSET, false));
        repository.save(account(tenantB, "Cash", AccountType.ASSET, true)); // different tenant, must not leak in
    }

    @Test
    void hasTenantScopesToOneTenant() {
        List<AccountEntity> results = repository.findAll(hasTenant(tenantA));

        assertThat(results).hasSize(3).allMatch(a -> a.getTenantId().equals(tenantA));
    }

    @Test
    void hasTypeFiltersByAccountType() {
        List<AccountEntity> results = repository.findAll(hasTenant(tenantA).and(hasType(AccountType.LIABILITY)));

        assertThat(results).extracting(AccountEntity::getName).containsExactly("Accounts Payable");
    }

    @Test
    void hasTypeWithNullIsANoOpFilter() {
        List<AccountEntity> results = repository.findAll(hasTenant(tenantA).and(hasType(null)));

        assertThat(results).hasSize(3);
    }

    @Test
    void isActiveFiltersOutInactiveAccounts() {
        List<AccountEntity> results = repository.findAll(hasTenant(tenantA).and(isActive(true)));

        assertThat(results).extracting(AccountEntity::getName)
                .containsExactlyInAnyOrder("Cash", "Accounts Payable");
    }

    @Test
    void nameContainsIsCaseInsensitive() {
        List<AccountEntity> results = repository.findAll(hasTenant(tenantA).and(nameContains("cash")));

        assertThat(results).extracting(AccountEntity::getName).containsExactly("Cash");
    }

    @Test
    void nameContainsWithBlankFragmentIsANoOpFilter() {
        List<AccountEntity> results = repository.findAll(hasTenant(tenantA).and(nameContains("  ")));

        assertThat(results).hasSize(3);
    }

    private static AccountEntity account(UUID tenantId, String name, AccountType type, boolean active) {
        AccountEntity entity = new AccountEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setName(name);
        entity.setType(type);
        entity.setCurrencyCode("XAF");
        entity.setActive(active);
        return entity;
    }
}
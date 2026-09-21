package com.np3.ledgerai.infrastructure.persistence.repository.specifications;

import com.np3.ledgerai.domain.model.JournalEntryStatus;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.EntryType;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryEntity;
import com.np3.ledgerai.infrastructure.persistence.Entity.JournalEntryLineEmbeddable;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.repository.JournalEntryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.JournalEntrySpecifications.createdFrom;
import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.JournalEntrySpecifications.createdTo;
import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.JournalEntrySpecifications.hasStatus;
import static com.np3.ledgerai.infrastructure.persistence.repository.specifications.JournalEntrySpecifications.hasTenant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest runs your real Liquibase migrations against the replaced H2 database
 * (spring-boot-starter-liquibase-test), so FK_LINE_ACCOUNT is genuinely enforced here --
 * every journal entry line below references an account that's actually persisted first.
 */
@DataJpaTest
class JournalEntrySpecificationsTest {

    @Autowired
    private JournalEntryJpaRepository repository;

    @Autowired
    private AccountJpaRepository accountRepository;

    private UUID tenantA;
    private UUID accountForTenantA;
    private Instant day1;
    private Instant day2;
    private Instant day3;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        accountForTenantA = persistAccount(tenantA);
        day1 = Instant.parse("2026-01-01T00:00:00Z");
        day2 = Instant.parse("2026-01-05T00:00:00Z");
        day3 = Instant.parse("2026-01-10T00:00:00Z");

        repository.save(entry(tenantA, accountForTenantA, JournalEntryStatus.DRAFT, day1));
        repository.save(entry(tenantA, accountForTenantA, JournalEntryStatus.POSTED, day2));
        repository.save(entry(tenantA, accountForTenantA, JournalEntryStatus.REVERSED, day3));

        UUID tenantB = UUID.randomUUID();
        UUID accountForTenantB = persistAccount(tenantB);
        repository.save(entry(tenantB, accountForTenantB, JournalEntryStatus.POSTED, day2)); // different tenant
    }

    @Test
    void hasTenantScopesToOneTenant() {
        List<JournalEntryEntity> results = repository.findAll(hasTenant(tenantA));

        assertThat(results).hasSize(3);
    }

    @Test
    void hasStatusFiltersByStatus() {
        List<JournalEntryEntity> results =
                repository.findAll(hasTenant(tenantA).and(hasStatus(JournalEntryStatus.POSTED)));

        assertThat(results).hasSize(1).allMatch(e -> e.getStatus() == JournalEntryStatus.POSTED);
    }

    @Test
    void hasStatusWithNullIsANoOpFilter() {
        List<JournalEntryEntity> results = repository.findAll(hasTenant(tenantA).and(hasStatus(null)));

        assertThat(results).hasSize(3);
    }

    @Test
    void createdFromAndCreatedToNarrowTheDateRange() {
        List<JournalEntryEntity> results = repository.findAll(
                hasTenant(tenantA).and(createdFrom(day2)).and(createdTo(day2)));

        assertThat(results).hasSize(1).allMatch(e -> e.getCreatedAt().equals(day2));
    }

    @Test
    void createdFromAloneIsAnOpenEndedLowerBound() {
        List<JournalEntryEntity> results = repository.findAll(hasTenant(tenantA).and(createdFrom(day2)));

        assertThat(results).hasSize(2); // day2 and day3
    }

    private UUID persistAccount(UUID tenantId) {
        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setTenantId(tenantId);
        account.setName("Test account");
        account.setType(AccountType.ASSET);
        account.setCurrencyCode("XAF");
        account.setActive(true);
        return accountRepository.save(account).getId();
    }

    private static JournalEntryEntity entry(UUID tenantId, UUID accountId, JournalEntryStatus status, Instant createdAt) {
        JournalEntryEntity entity = new JournalEntryEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setLines(List.of(line(accountId, BigDecimal.valueOf(100), EntryType.DEBIT),
                line(accountId, BigDecimal.valueOf(100), EntryType.CREDIT)));
        entity.setDescription("Test entry");
        entity.setCreatedAt(createdAt);
        entity.setCreatedBy(UUID.randomUUID());
        entity.setStatus(status);
        return entity;
    }

    private static JournalEntryLineEmbeddable line(UUID accountId, BigDecimal amount, EntryType entryType) {
        JournalEntryLineEmbeddable line = new JournalEntryLineEmbeddable();
        line.setAccountId(accountId);
        line.setAmount(amount);
        line.setCurrencyCode("XAF");
        line.setEntryType(entryType);
        return line;
    }
}
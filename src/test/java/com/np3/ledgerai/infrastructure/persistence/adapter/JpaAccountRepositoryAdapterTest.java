package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.criteria.AccountSearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.mappers.AccountMapper;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAccountRepositoryAdapterTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private AccountJpaRepository jpaRepository;

    private JpaAccountRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaAccountRepositoryAdapter(jpaRepository);
    }

    @Test
    void saveMapsToEntitySavesAndMapsBack() {
        Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
        when(jpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Account saved = adapter.save(account);

        assertThat(saved.id()).isEqualTo(account.id());
        assertThat(saved.name()).isEqualTo("Cash");
        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(account.id().value());
    }

    @Test
    void findByIdDelegatesAndMaps() {
        AccountEntity entity = AccountMapper.toEntity(Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF));
        when(jpaRepository.findByIdAndTenantId(entity.getId(), TENANT_ID.value()))
                .thenReturn(Optional.of(entity));

        Optional<Account> result = adapter.findById(TENANT_ID, AccountId.of(entity.getId()));

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Cash");
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        AccountId missingId = AccountId.generate();
        when(jpaRepository.findByIdAndTenantId(missingId.value(), TENANT_ID.value())).thenReturn(Optional.empty());

        assertThat(adapter.findById(TENANT_ID, missingId)).isEmpty();
    }

    @Test
    void existsByIdReflectsWhetherFindByIdAndTenantIdReturnsAValue() {
        AccountId id = AccountId.generate();
        when(jpaRepository.findByIdAndTenantId(id.value(), TENANT_ID.value()))
                .thenReturn(Optional.of(new AccountEntity()));

        assertThat(adapter.existsById(TENANT_ID, id)).isTrue();
    }

    @Test
    void searchMapsCriteriaToPageableAndWrapsTheResultingPage() {
        AccountEntity entity = AccountMapper.toEntity(Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF));
        Page<AccountEntity> page = new PageImpl<>(List.of(entity));
        when(jpaRepository.findAll(any(Specification.class), any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(page);

        PageResult<Account> result = adapter.search(TENANT_ID, new AccountSearchCriteria(null, null, null),
                new com.np3.ledgerai.domain.port.criteria.PageRequest(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).name()).isEqualTo("Cash");
        assertThat(result.page()).isEqualTo(page.getNumber());
        assertThat(result.size()).isEqualTo(page.getSize());
        assertThat(result.totalElements()).isEqualTo(page.getTotalElements());

        verify(jpaRepository).findAll(any(Specification.class),
                eq(org.springframework.data.domain.PageRequest.of(0, 20)));
    }

    @Test
    void findAllByTenantMapsEveryMatchingEntityUnpaginated() {
        AccountEntity cash = AccountMapper.toEntity(Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF));
        AccountEntity payable = AccountMapper.toEntity(
                Account.open(TENANT_ID, "Accounts Payable", AccountType.LIABILITY, XAF));
        when(jpaRepository.findAll(any(Specification.class))).thenReturn(List.of(cash, payable));

        List<Account> result = adapter.findAllByTenant(TENANT_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Account::name).containsExactlyInAnyOrder("Cash", "Accounts Payable");
    }
}
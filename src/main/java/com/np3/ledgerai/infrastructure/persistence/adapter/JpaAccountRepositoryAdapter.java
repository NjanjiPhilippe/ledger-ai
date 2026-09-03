package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.mappers.AccountMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaAccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    public JpaAccountRepositoryAdapter(AccountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Account save(Account account) {
        AccountEntity saved = jpaRepository.save(AccountMapper.toEntity(account));
        return AccountMapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(TenantId tenantId, AccountId id) {
        return jpaRepository.findByIdAndTenantId(id.value(), tenantId.value())
                .map(AccountMapper::toDomain);
    }

    @Override
    public List<Account> findAllByTenant(TenantId tenantId) {
        return jpaRepository.findAllByTenantId(tenantId.value()).stream()
                .map(AccountMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(TenantId tenantId, AccountId id) {
        return jpaRepository.existsByIdAndTenantId(id.value(), tenantId.value());
    }
}

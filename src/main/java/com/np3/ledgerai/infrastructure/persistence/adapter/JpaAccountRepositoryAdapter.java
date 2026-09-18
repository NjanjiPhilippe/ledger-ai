package com.np3.ledgerai.infrastructure.persistence.adapter;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.port.AccountRepository;
import com.np3.ledgerai.domain.port.criteria.AccountSearchCriteria;
import com.np3.ledgerai.domain.port.criteria.PageRequest;
import com.np3.ledgerai.domain.port.criteria.PageResult;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.repository.AccountJpaRepository;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import com.np3.ledgerai.infrastructure.persistence.mappers.AccountMapper;
import com.np3.ledgerai.infrastructure.persistence.repository.specifications.AccountSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

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
    public PageResult<Account> search(TenantId tenantId, AccountSearchCriteria criteria, PageRequest pageRequest) {
        Specification<AccountEntity> spec = Specification
                .where(AccountSpecifications.hasTenant(tenantId.value()))
                .and(AccountSpecifications.hasType(criteria.type()))
                .and(AccountSpecifications.isActive(criteria.active()))
                .and(AccountSpecifications.nameContains(criteria.nameContains()));

        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());

        Page<AccountEntity> page = jpaRepository.findAll(spec, springPageRequest);

        return new PageResult<>(
                page.getContent().stream().map(AccountMapper::toDomain).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    public boolean existsById(TenantId tenantId, AccountId id) {
        return jpaRepository.findByIdAndTenantId(id.value(), tenantId.value()).isPresent();
    }
}

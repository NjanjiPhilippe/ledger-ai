package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(TenantId tenantId, AccountId id);

    List<Account> findAllByTenant(TenantId tenantId);

    boolean existsById(TenantId tenantId, AccountId id);
}

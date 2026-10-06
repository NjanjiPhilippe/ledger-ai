package com.np3.ledgerai.infrastructure.persistence.mappers;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;

import java.util.Currency;

public final class AccountMapper {
    private AccountMapper() {
    }

    public static Account toDomain(AccountEntity entity) {
        return Account.reconstitute(
                AccountId.of(entity.getId()),
                TenantId.of(entity.getTenantId()),
                entity.getName(),
                entity.getType(),
                Currency.getInstance(entity.getCurrencyCode()),
                entity.isActive());
    }

    public static AccountEntity toEntity(Account account) {
        return new AccountEntity(
                account.id().value(),
                account.tenantId().value(),
                account.name(),
                account.type(),
                account.currency().getCurrencyCode(),
                account.active());
    }
}

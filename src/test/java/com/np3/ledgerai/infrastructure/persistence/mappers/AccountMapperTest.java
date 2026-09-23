package com.np3.ledgerai.infrastructure.persistence.mappers;

import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.infrastructure.persistence.Entity.AccountEntity;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountMapperTest {

    @Test
    void toEntityCopiesEveryField() {
        Account account = Account.reconstitute(
                AccountId.generate(), TenantId.of(UUID.randomUUID()), "Cash",
                AccountType.ASSET, Currency.getInstance("XAF"), true);

        AccountEntity entity = AccountMapper.toEntity(account);

        assertThat(entity.getId()).isEqualTo(account.id().value());
        assertThat(entity.getTenantId()).isEqualTo(account.tenantId().value());
        assertThat(entity.getName()).isEqualTo("Cash");
        assertThat(entity.getType()).isEqualTo(AccountType.ASSET);
        assertThat(entity.getCurrencyCode()).isEqualTo("XAF");
        assertThat(entity.isActive()).isTrue();
    }

    @Test
    void toDomainCopiesEveryField() {
        AccountEntity entity = new AccountEntity();
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        entity.setId(id);
        entity.setTenantId(tenantId);
        entity.setName("Accounts Payable");
        entity.setType(AccountType.LIABILITY);
        entity.setCurrencyCode("XAF");
        entity.setActive(false);

        Account account = AccountMapper.toDomain(entity);

        assertThat(account.id()).isEqualTo(AccountId.of(id));
        assertThat(account.tenantId()).isEqualTo(TenantId.of(tenantId));
        assertThat(account.name()).isEqualTo("Accounts Payable");
        assertThat(account.type()).isEqualTo(AccountType.LIABILITY);
        assertThat(account.currency()).isEqualTo(Currency.getInstance("XAF"));
        assertThat(account.active()).isFalse();
    }

    @Test
    void toEntityThenToDomainRoundTrips() {
        Account original = Account.open(TenantId.of(UUID.randomUUID()), "Cash", AccountType.ASSET,
                Currency.getInstance("XAF"));

        Account roundTripped = AccountMapper.toDomain(AccountMapper.toEntity(original));

        assertThat(roundTripped.id()).isEqualTo(original.id());
        assertThat(roundTripped.tenantId()).isEqualTo(original.tenantId());
        assertThat(roundTripped.name()).isEqualTo(original.name());
        assertThat(roundTripped.type()).isEqualTo(original.type());
        assertThat(roundTripped.currency()).isEqualTo(original.currency());
        assertThat(roundTripped.active()).isEqualTo(original.active());
    }
}

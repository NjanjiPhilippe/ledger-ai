package com.np3.ledgerai.domain.model;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;

import java.util.Currency;
import java.util.Objects;

public final class Account {
    private final AccountId id;
    private final TenantId tenantId;
    private String name;
    private final AccountType type;
    private final Currency currency;
    private boolean active;

    private Account(AccountId id, TenantId tenantId, String name, AccountType type, Currency currency,
                    boolean active) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.type = type;
        this.currency = currency;
        this.active = active;
    }

    public static Account open(TenantId tenantId, String name, AccountType type, Currency currency) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(currency, "currency");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Account name must not be blank");
        }
        return new Account(AccountId.generate(), tenantId, name, type, currency, true);
    }

    public static Account reconstitute(AccountId id, TenantId tenantId, String name, AccountType type,
                                       Currency currency, boolean active) {
        return new Account(id, tenantId, name, type, currency, active);
    }

    public void deactivate() {
        this.active = false;
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Account name must not be blank");
        }
        this.name = newName;
    }

    public AccountId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String name() {
        return name;
    }

    public AccountType type() {
        return type;
    }

    public Currency currency() {
        return currency;
    }

    public boolean active() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return id.equals(account.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

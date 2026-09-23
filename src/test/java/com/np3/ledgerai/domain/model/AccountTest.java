package com.np3.ledgerai.domain.model;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());
    private static final Currency XAF = Currency.getInstance("XAF");

    @Nested
    @DisplayName("open()")
    class Open {

        @Test
        @DisplayName("creates an active account with the given attributes")
        void createsAnActiveAccount() {
            Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);

            assertThat(account.tenantId()).isEqualTo(TENANT_ID);
            assertThat(account.name()).isEqualTo("Cash");
            assertThat(account.type()).isEqualTo(AccountType.ASSET);
            assertThat(account.currency()).isEqualTo(XAF);
            assertThat(account.active()).isTrue();
            assertThat(account.id()).isNotNull();
        }

        @Test
        @DisplayName("rejects a blank name")
        void rejectsBlankName() {
            assertThatThrownBy(() -> Account.open(TENANT_ID, "  ", AccountType.ASSET, XAF))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a null name")
        void rejectsNullName() {
            assertThatThrownBy(() -> Account.open(TENANT_ID, null, AccountType.ASSET, XAF))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("requires a non-null tenant, type and currency")
        void requiresMandatoryFields() {
            assertThatThrownBy(() -> Account.open(null, "Cash", AccountType.ASSET, XAF))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Account.open(TENANT_ID, "Cash", null, XAF))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Account.open(TENANT_ID, "Cash", AccountType.ASSET, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("rename()")
    class Rename {

        @Test
        @DisplayName("updates the name")
        void updatesTheName() {
            Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);

            account.rename("Petty Cash");

            assertThat(account.name()).isEqualTo("Petty Cash");
        }

        @Test
        @DisplayName("rejects a blank new name")
        void rejectsBlankNewName() {
            Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);

            assertThatThrownBy(() -> account.rename(" "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(account.name()).isEqualTo("Cash"); // unchanged
        }
    }

    @Nested
    @DisplayName("deactivate() / reactivate()")
    class ActivationToggle {

        @Test
        @DisplayName("deactivate() turns an active account inactive")
        void deactivateTurnsInactive() {
            Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);

            account.deactivate();

            assertThat(account.active()).isFalse();
        }

        @Test
        @DisplayName("reactivate() turns an inactive account active again")
        void reactivateTurnsActive() {
            Account account = Account.open(TENANT_ID, "Cash", AccountType.ASSET, XAF);
            account.deactivate();

            account.reactivate();

            assertThat(account.active()).isTrue();
        }
    }

    @Test
    @DisplayName("equals/hashCode are based on identity only")
    void equalityIsIdentityBased() {
        AccountId id = AccountId.generate();
        Account account = Account.reconstitute(id, TENANT_ID, "Cash", AccountType.ASSET, XAF, true);
        Account sameIdDifferentState = Account.reconstitute(id, TENANT_ID, "Renamed", AccountType.LIABILITY, XAF, false);

        assertThat(account).isEqualTo(sameIdDifferentState);
        assertThat(account).hasSameHashCodeAs(sameIdDifferentState);
    }
}
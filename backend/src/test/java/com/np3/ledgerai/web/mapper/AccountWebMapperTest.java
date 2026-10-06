package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.application.account.command.CreateAccountCommand;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.TenantId;
import com.np3.ledgerai.web.dto.account.AccountResponse;
import com.np3.ledgerai.web.dto.account.CreateAccountRequest;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountWebMapperTest {

    @Test
    void toCommandConvertsTheCurrencyCodeToACurrencyInstance() {
        CreateAccountRequest request = new CreateAccountRequest("Cash", AccountType.ASSET, "XAF");

        CreateAccountCommand command = AccountWebMapper.toCommand(request);

        assertThat(command.name()).isEqualTo("Cash");
        assertThat(command.type()).isEqualTo(AccountType.ASSET);
        assertThat(command.currency()).isEqualTo(Currency.getInstance("XAF"));
    }

    @Test
    void toResponseMapsEveryField() {
        Account account = Account.open(TenantId.of(UUID.randomUUID()), "Cash", AccountType.ASSET,
                Currency.getInstance("XAF"));

        AccountResponse response = AccountWebMapper.toResponse(account);

        assertThat(response.id()).isEqualTo(account.id().value());
        assertThat(response.name()).isEqualTo("Cash");
        assertThat(response.type()).isEqualTo(AccountType.ASSET);
        assertThat(response.currencyCode()).isEqualTo("XAF");
        assertThat(response.active()).isTrue();
    }
}

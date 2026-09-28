package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.application.account.command.CreateAccountCommand;
import com.np3.ledgerai.domain.model.Account;
import com.np3.ledgerai.web.dto.account.AccountResponse;
import com.np3.ledgerai.web.dto.account.CreateAccountRequest;

import java.util.Currency;

public final class AccountWebMapper {

    private AccountWebMapper() {
    }

    public static CreateAccountCommand toCommand(CreateAccountRequest request) {
        return new CreateAccountCommand(
                request.name(),
                request.type(),
                Currency.getInstance(request.currencyCode()));
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.id().value(),
                account.name(),
                account.type(),
                account.currency().getCurrencyCode(),
                account.active());
    }
}

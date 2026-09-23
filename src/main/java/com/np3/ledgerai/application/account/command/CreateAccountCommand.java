package com.np3.ledgerai.application.account.command;

import com.np3.ledgerai.domain.valueobject.AccountType;

import java.util.Currency;

public record CreateAccountCommand(String name, AccountType type, Currency currency) {
}

package com.np3.ledgerai.application.dto;

import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.Money;

public record TrialBalanceLine(AccountId accountId, String accountName, AccountType accountType, Money balance) {
}

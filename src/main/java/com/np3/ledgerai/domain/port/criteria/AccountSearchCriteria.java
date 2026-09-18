package com.np3.ledgerai.domain.port.criteria;

import com.np3.ledgerai.domain.valueobject.AccountType;

public record AccountSearchCriteria(AccountType type, Boolean active, String nameContains) {
}

package com.np3.ledgerai.application.advisor.useCase;

import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;

public interface GenerateAdviceUseCase {

    AdviceResult execute(FinancialSnapshot snapshot);
}
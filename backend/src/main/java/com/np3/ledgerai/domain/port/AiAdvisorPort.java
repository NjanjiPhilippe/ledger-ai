package com.np3.ledgerai.domain.port;

import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;

/**
 * Driven port for financial advisory analysis. The domain speaks only in
 * FinancialSnapshot / AdviceResult — it has no idea an LLM sits behind this
 * interface. Whichever provider is active (OpenAI, Anthropic, a future local
 * model...) is purely an infrastructure adapter concern.
 */
public interface AiAdvisorPort {

    AdviceResult analyze(FinancialSnapshot snapshot);
}
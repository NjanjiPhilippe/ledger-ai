package com.np3.ledgerai.application.advisor.useCase;

import com.np3.ledgerai.application.reporting.query.GetTrialBalanceQuery;
import com.np3.ledgerai.domain.exception.NoActivityToAnalyzeException;
import com.np3.ledgerai.domain.port.AiAdvisorPort;
import com.np3.ledgerai.domain.service.FinancialSnapshotComposer;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateLedgerAdviceUseCase {

    private final GetTrialBalanceQuery getTrialBalanceQuery;
    private final AiAdvisorPort aiAdvisorPort;

    @PreAuthorize("hasRole('VIEWER')")
    public AdviceResult execute() {
        var trialBalance = getTrialBalanceQuery.execute();
        FinancialSnapshot snapshot = FinancialSnapshotComposer.compose(trialBalance);

        if (snapshot.balances().isEmpty()) {
            // Avoid paying for an LLM call that can only say "there is nothing here".
            throw new NoActivityToAnalyzeException();
        }
        return aiAdvisorPort.analyze(snapshot);
    }
}
package com.np3.ledgerai.application.advisor.useCase;

import com.np3.ledgerai.domain.port.AiAdvisorPort;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GenerateAdviceService implements GenerateAdviceUseCase {

    private final AiAdvisorPort aiAdvisorPort;

    @Override
    @PreAuthorize("hasRole('VIEWER')")
    public AdviceResult execute(FinancialSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot must not be null");
        return aiAdvisorPort.analyze(snapshot);
    }
}

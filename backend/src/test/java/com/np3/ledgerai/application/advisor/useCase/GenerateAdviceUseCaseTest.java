package com.np3.ledgerai.application.advisor.useCase;

import com.np3.ledgerai.domain.port.AiAdvisorPort;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.domain.valueobject.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateAdviceUseCaseTest {

    private static final Currency XAF = Currency.getInstance("XAF");

    @Mock
    private AiAdvisorPort aiAdvisorPort;

    @Test
    void delegatesTheSnapshotToTheAdvisorPort() {
        var snapshot = new FinancialSnapshot(Instant.parse("2026-01-01T00:00:00Z"), "XAF", List.of(),
                Money.zero(XAF), Money.of(BigDecimal.ZERO, XAF));
        var advice = new AdviceResult(Instant.parse("2026-01-01T00:00:01Z"), "test", List.of());
        when(aiAdvisorPort.analyze(snapshot)).thenReturn(advice);

        assertThat(new GenerateAdviceUseCase(aiAdvisorPort).execute(snapshot)).isSameAs(advice);
    }

    @Test
    void rejectsANullSnapshot() {
        assertThatThrownBy(() -> new GenerateAdviceUseCase(aiAdvisorPort).execute(null))
                .isInstanceOf(NullPointerException.class);
    }
}

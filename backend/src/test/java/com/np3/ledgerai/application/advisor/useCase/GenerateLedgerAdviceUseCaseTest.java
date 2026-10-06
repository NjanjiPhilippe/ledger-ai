package com.np3.ledgerai.application.advisor.useCase;

import com.np3.ledgerai.domain.exception.NoActivityToAnalyzeException;
import com.np3.ledgerai.application.reporting.query.GetTrialBalanceQuery;
import com.np3.ledgerai.domain.port.AiAdvisorPort;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.TrialBalance;
import com.np3.ledgerai.domain.valueobject.TrialBalanceLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateLedgerAdviceUseCaseTest {

    private static final Currency XAF = Currency.getInstance("XAF");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private GetTrialBalanceQuery getTrialBalanceQuery;
    @Mock
    private AiAdvisorPort aiAdvisorPort;

    @Test
    void refusesToCallTheAdvisorWhenThereIsNoActivity() {
        when(getTrialBalanceQuery.execute()).thenReturn(trialBalance(List.of()));

        assertThatThrownBy(() -> new GenerateLedgerAdviceUseCase(getTrialBalanceQuery, aiAdvisorPort).execute())
                .isInstanceOf(NoActivityToAnalyzeException.class);

        verify(aiAdvisorPort, never()).analyze(any());
    }

    @Test
    void composesASnapshotFromTheTrialBalanceAndAsksTheAdvisor() {
        var line = new TrialBalanceLine(AccountId.generate(), "Cash", AccountType.ASSET,
                money(100), money(0), money(100));
        when(getTrialBalanceQuery.execute()).thenReturn(trialBalance(List.of(line)));
        var advice = new AdviceResult(NOW, "test", List.of());
        when(aiAdvisorPort.analyze(any())).thenReturn(advice);

        AdviceResult result = new GenerateLedgerAdviceUseCase(getTrialBalanceQuery, aiAdvisorPort).execute();

        assertThat(result).isSameAs(advice);
        ArgumentCaptor<FinancialSnapshot> captor = ArgumentCaptor.forClass(FinancialSnapshot.class);
        verify(aiAdvisorPort).analyze(captor.capture());
        assertThat(captor.getValue().balances()).extracting("accountName").containsExactly("Cash");
    }

    private static TrialBalance trialBalance(List<TrialBalanceLine> lines) {
        return new TrialBalance(NOW, XAF, lines, money(100), money(100));
    }

    private static Money money(int amount) {
        return Money.of(BigDecimal.valueOf(amount), XAF);
    }
}

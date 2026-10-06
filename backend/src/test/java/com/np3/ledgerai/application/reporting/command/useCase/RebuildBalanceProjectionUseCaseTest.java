package com.np3.ledgerai.application.reporting.command.useCase;

import com.np3.ledgerai.application.reporting.command.RebuildBalanceProjectionResult;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RebuildBalanceProjectionUseCaseTest {

    private static final TenantId TENANT_ID = TenantId.of(UUID.randomUUID());

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private BalanceProjectionRepository balanceProjectionRepository;

    private RebuildBalanceProjectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RebuildBalanceProjectionUseCase(journalEntryRepository, balanceProjectionRepository,
                () -> TENANT_ID);
    }

    @Test
    void replacesTheProjectionWithTheJournalTotalsAndCountsTheDriftedAccounts() {
        AccountId inSync = AccountId.generate();
        AccountId drifted = AccountId.generate();
        AccountId missing = AccountId.generate();   // posted in the journal, no projection row
        AccountId stale = AccountId.generate();     // projection row, nothing posted
        var expected = Map.of(
                inSync, totals(100, 0),
                drifted, totals(200, 50),
                missing, totals(0, 30));
        when(balanceProjectionRepository.lockAndFindAllTotals(TENANT_ID)).thenReturn(Map.of(
                inSync, new DebitCreditTotals(new BigDecimal("100.0000"), BigDecimal.ZERO), // scale differs only
                drifted, totals(190, 50),
                stale, totals(5, 0)));
        when(journalEntryRepository.sumPostedLinesByAccount(TENANT_ID)).thenReturn(expected);

        RebuildBalanceProjectionResult result = useCase.execute();

        assertThat(result.accountsRebuilt()).isEqualTo(3);
        assertThat(result.accountsCorrected()).isEqualTo(3); // drifted + missing + stale
        verify(balanceProjectionRepository).replaceAll(TENANT_ID, expected);
    }

    @Test
    void reportsNoDriftWhenTheProjectionAlreadyMatchesTheJournal() {
        AccountId account = AccountId.generate();
        when(balanceProjectionRepository.lockAndFindAllTotals(TENANT_ID)).thenReturn(Map.of(account, totals(10, 0)));
        when(journalEntryRepository.sumPostedLinesByAccount(TENANT_ID)).thenReturn(Map.of(account, totals(10, 0)));

        assertThat(useCase.execute().accountsCorrected()).isZero();
    }

    @Test
    void locksTheProjectionBeforeReadingTheJournal() {
        when(balanceProjectionRepository.lockAndFindAllTotals(TENANT_ID)).thenReturn(Map.of());
        when(journalEntryRepository.sumPostedLinesByAccount(TENANT_ID)).thenReturn(Map.of());

        useCase.execute();

        InOrder order = inOrder(balanceProjectionRepository, journalEntryRepository);
        order.verify(balanceProjectionRepository).lockAndFindAllTotals(TENANT_ID);
        order.verify(journalEntryRepository).sumPostedLinesByAccount(TENANT_ID);
        order.verify(balanceProjectionRepository).replaceAll(TENANT_ID, Map.of());
    }

    private static DebitCreditTotals totals(long debits, long credits) {
        return new DebitCreditTotals(BigDecimal.valueOf(debits), BigDecimal.valueOf(credits));
    }
}

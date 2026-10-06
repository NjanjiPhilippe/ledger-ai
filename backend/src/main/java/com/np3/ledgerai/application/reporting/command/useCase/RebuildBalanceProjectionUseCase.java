package com.np3.ledgerai.application.reporting.command.useCase;

import com.np3.ledgerai.application.reporting.command.RebuildBalanceProjectionResult;
import com.np3.ledgerai.domain.port.BalanceProjectionRepository;
import com.np3.ledgerai.domain.port.DebitCreditTotals;
import com.np3.ledgerai.domain.port.JournalEntryRepository;
import com.np3.ledgerai.domain.port.TenantContext;
import com.np3.ledgerai.domain.valueobject.AccountId;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Repairs the balance projection from the journal, which is the source of truth, and reports how many accounts
 * had drifted. The projection rows are locked first, so postings in flight wait for the rebuild to finish
 * and are then applied on top of the corrected rows.
 */
@Service
public class RebuildBalanceProjectionUseCase {

    private final JournalEntryRepository journalEntryRepository;
    private final BalanceProjectionRepository balanceProjectionRepository;
    private final TenantContext tenantContext;

    public RebuildBalanceProjectionUseCase(JournalEntryRepository journalEntryRepository,
                                           BalanceProjectionRepository balanceProjectionRepository,
                                           TenantContext tenantContext) {
        this.journalEntryRepository = journalEntryRepository;
        this.balanceProjectionRepository = balanceProjectionRepository;
        this.tenantContext = tenantContext;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public RebuildBalanceProjectionResult execute() {
        var tenantId = tenantContext.currentTenantId();

        // Order matters: lock first, then read the journal, so nothing can be posted in between.
        Map<AccountId, DebitCreditTotals> current = balanceProjectionRepository.lockAndFindAllTotals(tenantId);
        Map<AccountId, DebitCreditTotals> expected = journalEntryRepository.sumPostedLinesByAccount(tenantId);

        Set<AccountId> accounts = new HashSet<>(current.keySet());
        accounts.addAll(expected.keySet());
        int corrected = (int) accounts.stream()
                .filter(account -> !current.getOrDefault(account, DebitCreditTotals.zero())
                        .sameAmountsAs(expected.getOrDefault(account, DebitCreditTotals.zero())))
                .count();

        balanceProjectionRepository.replaceAll(tenantId, expected);
        return new RebuildBalanceProjectionResult(expected.size(), corrected);
    }
}

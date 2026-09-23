package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.domain.valueobject.TrialBalance;
import com.np3.ledgerai.domain.valueobject.TrialBalanceLine;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceLineResponse;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceResponse;

import java.util.List;

public final class TrialBalanceWebMapper {

    private TrialBalanceWebMapper() {
    }

    public static TrialBalanceResponse toResponse(TrialBalance trialBalance) {
        List<TrialBalanceLineResponse> lines = trialBalance.lines().stream()
                .map(TrialBalanceWebMapper::toLineResponse)
                .toList();

        return new TrialBalanceResponse(
                trialBalance.generatedAt(),
                trialBalance.currency().getCurrencyCode(),
                lines,
                trialBalance.totalDebits().amount(),
                trialBalance.totalCredits().amount(),
                trialBalance.balanced()
        );
    }

    private static TrialBalanceLineResponse toLineResponse(TrialBalanceLine line) {
        return new TrialBalanceLineResponse(
                // TODO: aligner sur le vrai accesseur d'AccountId (ex. .value() ou .id())
                // pour extraire l'UUID brut — même pattern que dans AccountWebMapper existant.
                line.accountId().value(),
                line.accountName(),
                line.accountType().name(),
                line.totalDebits().amount(),
                line.totalCredits().amount(),
                line.balance().amount()
        );
    }
}

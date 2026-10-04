package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.domain.valueobject.TrialBalance;
import com.np3.ledgerai.domain.valueobject.TrialBalanceLine;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceLineResponse;
import com.np3.ledgerai.web.dto.reporting.TrialBalanceResponse;

import java.util.List;

public final class ReportWebMapper {

    private ReportWebMapper() {
    }

    public static TrialBalanceResponse toResponse(TrialBalance trialBalance) {
        List<TrialBalanceLineResponse> lines = trialBalance.lines().stream()
                .map(ReportWebMapper::toLineResponse)
                .toList();

        return new TrialBalanceResponse(
                trialBalance.generatedAt(),
                trialBalance.currency().getCurrencyCode(),
                lines,
                trialBalance.totalDebits().amount(),
                trialBalance.totalCredits().amount(),
                trialBalance.balanced());
    }

    private static TrialBalanceLineResponse toLineResponse(TrialBalanceLine line) {
        return new TrialBalanceLineResponse(
                line.accountId().value(),
                line.accountName(),
                line.accountType().name(),
                line.totalDebits().amount(),
                line.totalCredits().amount(),
                line.balance().amount());
    }
}
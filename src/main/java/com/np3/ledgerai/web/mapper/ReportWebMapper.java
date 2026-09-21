package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.application.dto.TrialBalance;
import com.np3.ledgerai.application.dto.TrialBalanceLine;
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

        String currencyCode = trialBalance.currency() == null ? null : trialBalance.currency().getCurrencyCode();

        return new TrialBalanceResponse(
                lines,
                trialBalance.totalDebits(),
                trialBalance.totalCredits(),
                currencyCode,
                trialBalance.balanced());
    }

    private static TrialBalanceLineResponse toLineResponse(TrialBalanceLine line) {
        return new TrialBalanceLineResponse(
                line.accountId().value(),
                line.accountName(),
                line.accountType(),
                line.balance().amount());
    }
}
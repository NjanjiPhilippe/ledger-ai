package com.np3.ledgerai.web.mapper;

import com.np3.ledgerai.domain.valueobject.AccountBalanceLine;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.web.dto.advisor.AccountBalanceLineRequest;
import com.np3.ledgerai.web.dto.advisor.AdviceResponse;
import com.np3.ledgerai.web.dto.advisor.AnalyzeSnapshotRequest;
import com.np3.ledgerai.web.dto.advisor.RecommendationResponse;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

public final class AdvisorWebMapper {

    private AdvisorWebMapper() {
    }

    public static FinancialSnapshot toSnapshot(AnalyzeSnapshotRequest request) {
        Currency currency = Currency.getInstance(request.currencyCode());

        List<AccountBalanceLine> lines = request.balances().stream()
                .map(line -> toBalanceLine(line, currency))
                .toList();

        return new FinancialSnapshot(
                Instant.now(),
                request.currencyCode(),
                lines,
                Money.of(request.totalDebits(), currency),
                Money.of(request.totalCredits(), currency)
        );
    }

    private static AccountBalanceLine toBalanceLine(AccountBalanceLineRequest line, Currency currency) {
        return new AccountBalanceLine(
                AccountId.of(line.accountId()),
                line.accountName(),
                line.accountType(),
                Money.of(line.balance(), currency)
        );
    }

    public static AdviceResponse toResponse(AdviceResult result) {
        List<RecommendationResponse> recommendations = result.recommendations().stream()
                .map(r -> new RecommendationResponse(
                        r.title(),
                        r.detail(),
                        r.category().name(),
                        r.severity().name()
                ))
                .toList();

        return new AdviceResponse(result.generatedAt(), result.provider(), recommendations);
    }
}
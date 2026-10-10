package com.np3.ledgerai.infrastructure.advisor.stub;

import com.np3.ledgerai.domain.valueobject.AccountBalanceLine;
import com.np3.ledgerai.domain.valueobject.AccountId;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.AdviceCategory;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.AdviceSeverity;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.domain.valueobject.Money;
import com.np3.ledgerai.domain.valueobject.Recommendation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StubAdvisorAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-10T08:00:00Z");
    private static final Currency XAF = Currency.getInstance("XAF");

    private final StubAdvisorAdapter advisor = new StubAdvisorAdapter(Clock.fixed(NOW, ZoneOffset.UTC));

    private static Money xaf(long amount) {
        return Money.of(BigDecimal.valueOf(amount), XAF);
    }

    private static AccountBalanceLine line(String name, AccountType type, long balance) {
        return new AccountBalanceLine(AccountId.generate(), name, type, xaf(balance));
    }

    private static FinancialSnapshot snapshot(long debits, long credits, AccountBalanceLine... lines) {
        return new FinancialSnapshot(NOW, "XAF", List.of(lines), xaf(debits), xaf(credits));
    }

    private static List<String> titles(AdviceResult result) {
        return result.recommendations().stream().map(Recommendation::title).toList();
    }

    private static Recommendation find(AdviceResult result, String title) {
        return result.recommendations().stream().filter(r -> r.title().equals(title)).findFirst().orElseThrow();
    }

    @Test
    void namesItselfAsTheStubProviderAndAlwaysSaysItIsNotAnAi() {
        AdviceResult result = advisor.analyze(snapshot(0, 0));

        assertThat(result.provider()).isEqualTo("stub");
        assertThat(result.generatedAt()).isEqualTo(NOW);
        assertThat(result.recommendations()).last()
                .extracting(Recommendation::title).isEqualTo("Conseiller de démonstration");
    }

    @Test
    void saysThereIsNothingToAnalyzeOnAnEmptyLedger() {
        AdviceResult result = advisor.analyze(snapshot(0, 0, line("Cash", AccountType.ASSET, 0)));

        assertThat(titles(result)).containsExactly("Aucune écriture comptabilisée", "Conseiller de démonstration");
    }

    @Test
    void flagsAnUnbalancedLedgerAsCritical() {
        AdviceResult result = advisor.analyze(snapshot(1000, 900, line("Cash", AccountType.ASSET, 1000)));

        Recommendation recommendation = find(result, "Le grand livre est déséquilibré");
        assertThat(recommendation.severity()).isEqualTo(AdviceSeverity.CRITICAL);
        assertThat(recommendation.category()).isEqualTo(AdviceCategory.RISK);
    }

    @Test
    void flagsExpensesAboveRevenueAsCritical() {
        AdviceResult result = advisor.analyze(snapshot(2000, 2000,
                line("Sales", AccountType.REVENUE, 1000), line("Rent", AccountType.EXPENSE, 1500)));

        Recommendation recommendation = find(result, "Les charges dépassent les produits");
        assertThat(recommendation.severity()).isEqualTo(AdviceSeverity.CRITICAL);
        assertThat(recommendation.detail()).contains("-500");
    }

    @Test
    void warnsAboutAThinMargin() {
        AdviceResult result = advisor.analyze(snapshot(2000, 2000,
                line("Sales", AccountType.REVENUE, 1000), line("Rent", AccountType.EXPENSE, 900)));

        Recommendation recommendation = find(result, "La marge est mince");
        assertThat(recommendation.severity()).isEqualTo(AdviceSeverity.WARNING);
        assertThat(recommendation.detail()).contains("90.0 %");
    }

    @Test
    void notesAHealthyResultAsInformation() {
        AdviceResult result = advisor.analyze(snapshot(2000, 2000,
                line("Sales", AccountType.REVENUE, 1000), line("Rent", AccountType.EXPENSE, 400)));

        Recommendation recommendation = find(result, "Le résultat est positif");
        assertThat(recommendation.severity()).isEqualTo(AdviceSeverity.INFO);
        assertThat(recommendation.detail()).contains("600").contains("60.0 %");
    }

    @Test
    void warnsWhenLiabilitiesExceedAssets() {
        AdviceResult result = advisor.analyze(snapshot(1500, 1500,
                line("Cash", AccountType.ASSET, 500), line("Suppliers", AccountType.LIABILITY, 900)));

        Recommendation recommendation = find(result, "Les dettes dépassent l'actif");
        assertThat(recommendation.category()).isEqualTo(AdviceCategory.LIQUIDITY);
        assertThat(recommendation.severity()).isEqualTo(AdviceSeverity.WARNING);
    }

    @Test
    void suggestsPuttingIdleFundsToWorkWhenThereIsNoDebt() {
        AdviceResult result = advisor.analyze(snapshot(1000, 1000, line("Cash", AccountType.ASSET, 1000)));

        assertThat(find(result, "Aucune dette enregistrée").category()).isEqualTo(AdviceCategory.GROWTH);
    }

    @Test
    void warnsWhenOneRevenueAccountDominates() {
        AdviceResult result = advisor.analyze(snapshot(3000, 3000,
                line("Consulting", AccountType.REVENUE, 900), line("Sales", AccountType.REVENUE, 100)));

        Recommendation recommendation = find(result, "Les produits dépendent d'un seul compte");
        assertThat(recommendation.detail()).contains("Consulting").contains("90.0 %");
    }

    @Test
    void doesNotCallASingleRevenueAccountADependency() {
        AdviceResult result = advisor.analyze(snapshot(3000, 3000, line("Sales", AccountType.REVENUE, 1000)));

        assertThat(titles(result)).doesNotContain("Les produits dépendent d'un seul compte");
    }

    @Test
    void flagsAnAccountAgainstItsNature() {
        AdviceResult result = advisor.analyze(snapshot(1000, 1000,
                line("Cash", AccountType.ASSET, -200), line("Sales", AccountType.REVENUE, 1200)));

        Recommendation recommendation = find(result, "Solde inhabituel sur « Cash »");
        assertThat(recommendation.category()).isEqualTo(AdviceCategory.COMPLIANCE);
    }

    @Test
    void givesTheSameAdviceForTheSameFigures() {
        FinancialSnapshot snapshot = snapshot(2000, 2000,
                line("Sales", AccountType.REVENUE, 1000), line("Rent", AccountType.EXPENSE, 400));

        assertThat(advisor.analyze(snapshot)).isEqualTo(advisor.analyze(snapshot));
    }
}

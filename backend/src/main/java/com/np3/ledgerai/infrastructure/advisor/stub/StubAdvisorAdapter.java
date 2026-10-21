package com.np3.ledgerai.infrastructure.advisor.stub;

import com.np3.ledgerai.domain.port.AiAdvisorPort;
import com.np3.ledgerai.domain.valueobject.AccountBalanceLine;
import com.np3.ledgerai.domain.valueobject.AccountType;
import com.np3.ledgerai.domain.valueobject.AdviceCategory;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.AdviceSeverity;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.domain.valueobject.Recommendation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * An advisor that needs no network and no API key: fixed rules over the trial balance.
 * <p>
 * It exists so that the project runs, demos and tests without paying a provider. It is not an AI: its
 * recommendations are simple ratios, and the last one says so. Select it with
 * {@code ledgerai.advisor.provider=stub}.
 */
@Component
@ConditionalOnProperty(prefix = "ledgerai.advisor", name = "provider", havingValue = "stub")
public class StubAdvisorAdapter implements AiAdvisorPort {

    private static final String PROVIDER_NAME = "stub";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** Expenses above this share of revenue leave a thin margin. */
    private static final BigDecimal THIN_MARGIN_EXPENSE_SHARE = new BigDecimal("0.80");
    /** One revenue account above this share of all revenue is a dependency worth naming. */
    private static final BigDecimal CONCENTRATION_SHARE = new BigDecimal("0.80");

    private final Clock clock;

    public StubAdvisorAdapter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public AdviceResult analyze(FinancialSnapshot snapshot) {
        List<Recommendation> recommendations = new ArrayList<>();

        BigDecimal assets = total(snapshot, AccountType.ASSET);
        BigDecimal liabilities = total(snapshot, AccountType.LIABILITY);
        BigDecimal revenue = total(snapshot, AccountType.REVENUE);
        BigDecimal expenses = total(snapshot, AccountType.EXPENSE);

        if (snapshot.totalDebits().amount().compareTo(snapshot.totalCredits().amount()) != 0) {
            recommendations.add(new Recommendation(
                    "Le grand livre est déséquilibré",
                    "Le total des débits (" + snapshot.totalDebits().amount().toPlainString() + " "
                            + snapshot.currencyCode() + ") diffère du total des crédits ("
                            + snapshot.totalCredits().amount().toPlainString() + "). Vérifiez les écritures "
                            + "comptabilisées et reconstruisez les soldes si besoin.",
                    AdviceCategory.RISK, AdviceSeverity.CRITICAL));
        }

        boolean empty = snapshot.balances().stream().allMatch(line -> line.balance().amount().signum() == 0);
        if (empty) {
            recommendations.add(new Recommendation(
                    "Aucune écriture comptabilisée",
                    "Les soldes sont tous à zéro. Enregistrez et comptabilisez des écritures pour obtenir une analyse.",
                    AdviceCategory.GENERAL, AdviceSeverity.INFO));
        } else {
            addProfitability(recommendations, snapshot, revenue, expenses);
            addLiquidity(recommendations, snapshot, assets, liabilities);
            addConcentration(recommendations, snapshot, revenue);
            addUnusualBalances(recommendations, snapshot);
        }

        recommendations.add(new Recommendation(
                "Conseiller de démonstration",
                "Ces recommandations viennent de règles fixes appliquées à la balance, pas d'un modèle d'IA. "
                        + "Configurez un fournisseur (Anthropic ou OpenAI) pour une analyse réelle.",
                AdviceCategory.GENERAL, AdviceSeverity.INFO));

        return new AdviceResult(clock.instant(), PROVIDER_NAME, recommendations);
    }

    private void addProfitability(List<Recommendation> out, FinancialSnapshot snapshot,
                                  BigDecimal revenue, BigDecimal expenses) {
        String currency = snapshot.currencyCode();
        BigDecimal result = revenue.subtract(expenses);
        if (result.signum() < 0) {
            out.add(new Recommendation(
                    "Les charges dépassent les produits",
                    "Le résultat est négatif : " + result.toPlainString() + " " + currency
                            + " (produits " + revenue.toPlainString() + ", charges " + expenses.toPlainString()
                            + "). Identifiez les charges à réduire ou les produits manquants.",
                    AdviceCategory.PROFITABILITY, AdviceSeverity.CRITICAL));
        } else if (revenue.signum() > 0
                && expenses.compareTo(revenue.multiply(THIN_MARGIN_EXPENSE_SHARE)) > 0) {
            out.add(new Recommendation(
                    "La marge est mince",
                    "Les charges représentent " + percent(expenses, revenue) + " % des produits : le résultat "
                            + "n'est que de " + result.toPlainString() + " " + currency + ".",
                    AdviceCategory.PROFITABILITY, AdviceSeverity.WARNING));
        } else if (revenue.signum() > 0) {
            out.add(new Recommendation(
                    "Le résultat est positif",
                    "Les produits dépassent les charges de " + result.toPlainString() + " " + currency
                            + " (" + percent(result, revenue) + " % des produits).",
                    AdviceCategory.PROFITABILITY, AdviceSeverity.INFO));
        }
    }

    private void addLiquidity(List<Recommendation> out, FinancialSnapshot snapshot,
                              BigDecimal assets, BigDecimal liabilities) {
        if (liabilities.signum() > 0 && liabilities.compareTo(assets) > 0) {
            out.add(new Recommendation(
                    "Les dettes dépassent l'actif",
                    "Les dettes (" + liabilities.toPlainString() + " " + snapshot.currencyCode()
                            + ") sont supérieures à l'actif (" + assets.toPlainString()
                            + "). Planifiez les encaissements et les échéances.",
                    AdviceCategory.LIQUIDITY, AdviceSeverity.WARNING));
        } else if (assets.signum() > 0 && liabilities.signum() == 0) {
            out.add(new Recommendation(
                    "Aucune dette enregistrée",
                    "L'actif atteint " + assets.toPlainString() + " " + snapshot.currencyCode()
                            + " sans dette : examinez si une partie peut être placée ou investie.",
                    AdviceCategory.GROWTH, AdviceSeverity.INFO));
        }
    }

    private void addConcentration(List<Recommendation> out, FinancialSnapshot snapshot, BigDecimal revenue) {
        if (revenue.signum() <= 0) {
            return;
        }
        snapshot.balances().stream()
                .filter(line -> line.accountType() == AccountType.REVENUE)
                .filter(line -> line.balance().amount().compareTo(revenue.multiply(CONCENTRATION_SHARE)) >= 0)
                .filter(line -> snapshot.balances().stream()
                        .filter(other -> other.accountType() == AccountType.REVENUE).count() > 1)
                .findFirst()
                .ifPresent(line -> out.add(new Recommendation(
                        "Les produits dépendent d'un seul compte",
                        "« " + line.accountName() + " » représente " + percent(line.balance().amount(), revenue)
                                + " % des produits. Une baisse sur cette source pèserait fortement sur le résultat.",
                        AdviceCategory.RISK, AdviceSeverity.WARNING)));
    }

    private void addUnusualBalances(List<Recommendation> out, FinancialSnapshot snapshot) {
        for (AccountBalanceLine line : snapshot.balances()) {
            if (line.balance().isNegative()) {
                out.add(new Recommendation(
                        "Solde inhabituel sur « " + line.accountName() + " »",
                        "Le compte est à contre-sens de sa nature (solde " + line.balance().amount().toPlainString()
                                + " " + snapshot.currencyCode() + "). Vérifiez les écritures qui l'affectent.",
                        AdviceCategory.COMPLIANCE, AdviceSeverity.WARNING));
            }
        }
    }

    private static BigDecimal total(FinancialSnapshot snapshot, AccountType type) {
        return snapshot.balances().stream()
                .filter(line -> line.accountType() == type)
                .map(line -> line.balance().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String percent(BigDecimal part, BigDecimal whole) {
        return part.multiply(HUNDRED).divide(whole, 1, RoundingMode.HALF_UP).toPlainString();
    }
}

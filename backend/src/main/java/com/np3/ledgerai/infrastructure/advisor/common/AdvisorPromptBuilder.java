package com.np3.ledgerai.infrastructure.advisor.common;

import com.np3.ledgerai.domain.valueobject.AccountBalanceLine;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import org.springframework.stereotype.Component;

/**
 * Turns a financial snapshot into a prompt for the LLM.
 * It's created in the infrastructure folder since the
 * domain shouldn't know what a "Prompt" is.
 * */
@Component
public class AdvisorPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            Tu es un conseiller financier pour des PME dans la zone OHADA. \
            Analyse le snapshot comptable fourni (comptabilité en partie double) \
            et propose des recommandations concrètes, priorisées par sévérité.
            Réponds UNIQUEMENT avec un tableau JSON, sans texte autour ni balises markdown, au format :
            [
              {"title": "...", "detail": "...", "category": "LIQUIDITY|PROFITABILITY|RISK|COMPLIANCE|GROWTH|GENERAL", "severity": "INFO|WARNING|CRITICAL"}
            ]
            """;

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(FinancialSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Devise : ").append(snapshot.currencyCode()).append('\n');
        sb.append("Généré le : ").append(snapshot.generatedAt()).append('\n');
        sb.append("Total débits : ").append(snapshot.totalDebits().amount()).append('\n');
        sb.append("Total crédits : ").append(snapshot.totalCredits().amount()).append('\n');
        sb.append("Soldes par compte :\n");
        for (AccountBalanceLine line : snapshot.balances()) {
            sb.append("- ")
                    .append(line.accountName())
                    .append(" (").append(line.accountType()).append(") : ")
                    .append(line.balance().amount())
                    .append('\n');
        }
        return sb.toString();
    }
}
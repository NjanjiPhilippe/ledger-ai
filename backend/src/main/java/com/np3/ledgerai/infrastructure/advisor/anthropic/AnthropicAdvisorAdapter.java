package com.np3.ledgerai.infrastructure.advisor.anthropic;

import com.np3.ledgerai.domain.exception.AiAdvisorException;
import com.np3.ledgerai.domain.port.AiAdvisorPort;
import com.np3.ledgerai.domain.valueobject.AdviceResult;
import com.np3.ledgerai.domain.valueobject.FinancialSnapshot;
import com.np3.ledgerai.infrastructure.advisor.common.AdvisorPromptBuilder;
import com.np3.ledgerai.infrastructure.advisor.common.RecommendationJsonParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(AnthropicAdvisorProperties.class)
@ConditionalOnProperty(prefix = "ledgerai.advisor", name = "provider", havingValue = "anthropic", matchIfMissing = true)
public class AnthropicAdvisorAdapter implements AiAdvisorPort {

    private static final String PROVIDER_NAME = "anthropic";
    /** Room for the model's own reasoning as well as the JSON answer: a cap hit mid-answer leaves unreadable JSON. */
    private static final int MAX_TOKENS = 4096;

    private final AnthropicAdvisorProperties properties;
    private final AdvisorPromptBuilder promptBuilder;
    private final RecommendationJsonParser recommendationJsonParser;
    private final RestClient restClient;
    private final Clock clock;

    public AnthropicAdvisorAdapter(AnthropicAdvisorProperties properties,
                                   AdvisorPromptBuilder promptBuilder,
                                   RecommendationJsonParser recommendationJsonParser,
                                   Clock clock) {
        this.clock = clock;
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.recommendationJsonParser = recommendationJsonParser;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    @Override
    public AdviceResult analyze(FinancialSnapshot snapshot) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "max_tokens", MAX_TOKENS,
                "system", promptBuilder.systemPrompt(),
                "messages", List.of(
                        Map.of("role", "user", "content", promptBuilder.userPrompt(snapshot))
                )
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/messages")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String content = extractContent(response);
            return new AdviceResult(clock.instant(), PROVIDER_NAME, recommendationJsonParser.parse(content));
        } catch (AiAdvisorException e) {
            throw e;
        } catch (Exception e) {
            throw new AiAdvisorException("Échec de l'appel à Anthropic", e);
        }
    }

    /**
     * The text of the answer. Current Claude models may put other blocks (thinking, for instance) before the text,
     * so the first block is not necessarily the answer: every text block is read, in order.
     */
    @SuppressWarnings("unchecked")
    static String extractContent(Map<String, Object> response) {
        if (response == null) {
            throw new AiAdvisorException("Réponse vide d'Anthropic");
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) {
            throw new AiAdvisorException("Aucun contenu retourné par Anthropic");
        }
        String text = content.stream()
                .filter(block -> "text".equals(block.get("type")) && block.get("text") instanceof String)
                .map(block -> (String) block.get("text"))
                .reduce("", String::concat);
        if (text.isBlank()) {
            throw new AiAdvisorException("Aucun texte dans la réponse d'Anthropic (arrêt : "
                    + response.get("stop_reason") + ")");
        }
        return text;
    }
}

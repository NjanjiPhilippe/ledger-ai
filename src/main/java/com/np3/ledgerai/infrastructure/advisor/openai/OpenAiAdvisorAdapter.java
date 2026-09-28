package com.np3.ledgerai.infrastructure.advisor.openai;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(OpenAiAdvisorProperties.class)
@ConditionalOnProperty(prefix = "ledgerai.advisor", name = "provider", havingValue = "openai")
public class OpenAiAdvisorAdapter implements AiAdvisorPort {

    private static final String PROVIDER_NAME = "openai";

    private final OpenAiAdvisorProperties properties;
    private final AdvisorPromptBuilder promptBuilder;
    private final RecommendationJsonParser recommendationJsonParser;
    private final RestClient restClient;

    public OpenAiAdvisorAdapter(OpenAiAdvisorProperties properties,
                                AdvisorPromptBuilder promptBuilder,
                                RecommendationJsonParser recommendationJsonParser) {
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.recommendationJsonParser = recommendationJsonParser;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
    }

    @Override
    public AdviceResult analyze(FinancialSnapshot snapshot) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", promptBuilder.systemPrompt()),
                        Map.of("role", "user", "content", promptBuilder.userPrompt(snapshot))
                ),
                "temperature", 0.2
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            String content = extractContent(response);
            return new AdviceResult(Instant.now(), PROVIDER_NAME, recommendationJsonParser.parse(content));
        } catch (AiAdvisorException e) {
            throw e;
        } catch (Exception e) {
            throw new AiAdvisorException("Échec de l'appel à OpenAI", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            throw new AiAdvisorException("Réponse vide d'OpenAI");
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AiAdvisorException("Aucun choix retourné par OpenAI");
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}

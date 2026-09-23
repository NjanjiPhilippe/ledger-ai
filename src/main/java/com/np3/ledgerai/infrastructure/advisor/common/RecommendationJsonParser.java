package com.np3.ledgerai.infrastructure.advisor.common;

import com.np3.ledgerai.domain.exception.AiAdvisorException;
import com.np3.ledgerai.domain.valueobject.AdviceCategory;
import com.np3.ledgerai.domain.valueobject.AdviceSeverity;
import com.np3.ledgerai.domain.valueobject.Recommendation;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Untrusted-output boundary: an LLM response is never guaranteed to be clean
 * JSON or to use exactly the enum values we asked for, so every failure here
 * is swallowed into an AiAdvisorException rather than propagating a raw
 * parsing exception, and unknown enum values fall back to a safe default
 * instead of blowing up the whole request.
 */
@Component
public class RecommendationJsonParser {

    private final ObjectMapper objectMapper;

    public RecommendationJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Recommendation> parse(String rawJson) {
        try {
            String cleaned = stripCodeFences(rawJson);
            List<RecommendationJson> parsed = objectMapper.readValue(
                    cleaned,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, RecommendationJson.class)
            );
            return parsed.stream()
                    .map(this::toDomain)
                    .toList();
        } catch (Exception e) {
            throw new AiAdvisorException("Réponse IA illisible : impossible de parser les recommandations", e);
        }
    }

    private Recommendation toDomain(RecommendationJson json) {
        return new Recommendation(
                json.title(),
                json.detail(),
                parseEnumOrDefault(json.category(), AdviceCategory.class, AdviceCategory.GENERAL),
                parseEnumOrDefault(json.severity(), AdviceSeverity.class, AdviceSeverity.INFO)
        );
    }

    private <T extends Enum<T>> T parseEnumOrDefault(String raw, Class<T> enumType, T fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String stripCodeFences(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}

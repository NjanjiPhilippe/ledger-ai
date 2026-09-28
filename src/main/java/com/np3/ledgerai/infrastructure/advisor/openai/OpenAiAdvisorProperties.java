package com.np3.ledgerai.infrastructure.advisor.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerai.advisor.openai")
public record OpenAiAdvisorProperties(
        String apiKey,
        String model,
        String baseUrl
) {
    public OpenAiAdvisorProperties {
        model = (model == null || model.isBlank()) ? "gpt-4o-mini" : model;
        baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openai.com/v1" : baseUrl;
    }
}

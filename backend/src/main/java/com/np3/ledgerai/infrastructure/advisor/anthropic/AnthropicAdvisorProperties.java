package com.np3.ledgerai.infrastructure.advisor.anthropic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledgerai.advisor.anthropic")
public record AnthropicAdvisorProperties(
        String apiKey,
        String model,
        String baseUrl
) {
    public AnthropicAdvisorProperties {
        model = (model == null || model.isBlank()) ? "claude-sonnet-4-6" : model;
        baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://api.anthropic.com/v1" : baseUrl;
    }
}

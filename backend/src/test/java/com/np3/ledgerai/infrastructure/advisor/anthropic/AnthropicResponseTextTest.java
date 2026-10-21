package com.np3.ledgerai.infrastructure.advisor.anthropic;

import com.np3.ledgerai.domain.exception.AiAdvisorException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicResponseTextTest {

    private static Map<String, Object> text(String value) {
        return Map.of("type", "text", "text", value);
    }

    @Test
    void readsTheTextOfASingleBlock() {
        assertThat(AnthropicAdvisorAdapter.extractContent(Map.of("content", List.of(text("[]"))))).isEqualTo("[]");
    }

    @Test
    void skipsThinkingBlocksThatCurrentModelsPutFirst() {
        Map<String, Object> thinking = Map.of("type", "thinking", "thinking", "", "signature", "abc");

        String result = AnthropicAdvisorAdapter.extractContent(Map.of("content", List.of(thinking, text("[{\"a\":1}]"))));

        assertThat(result).isEqualTo("[{\"a\":1}]");
    }

    @Test
    void joinsSeveralTextBlocksInOrder() {
        String result = AnthropicAdvisorAdapter.extractContent(Map.of("content", List.of(text("[1,"), text("2]"))));

        assertThat(result).isEqualTo("[1,2]");
    }

    @Test
    void failsClearlyWhenThereIsNoTextAndSaysWhyTheModelStopped() {
        Map<String, Object> thinking = Map.of("type", "thinking", "thinking", "reasoning");

        assertThatThrownBy(() -> AnthropicAdvisorAdapter.extractContent(
                Map.of("content", List.of(thinking), "stop_reason", "max_tokens")))
                .isInstanceOf(AiAdvisorException.class)
                .hasMessageContaining("max_tokens");
    }

    @Test
    void failsOnAnEmptyOrMissingAnswer() {
        assertThatThrownBy(() -> AnthropicAdvisorAdapter.extractContent(null)).isInstanceOf(AiAdvisorException.class);
        assertThatThrownBy(() -> AnthropicAdvisorAdapter.extractContent(Map.of("content", List.of())))
                .isInstanceOf(AiAdvisorException.class);
    }
}

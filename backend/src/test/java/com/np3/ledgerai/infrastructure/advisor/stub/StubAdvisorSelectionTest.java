package com.np3.ledgerai.infrastructure.advisor.stub;

import com.np3.ledgerai.domain.port.AiAdvisorPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/** Which advisor is active depends only on ledgerai.advisor.provider. */
class StubAdvisorSelectionTest {

    @Configuration
    @EnableConfigurationProperties
    @Import(StubAdvisorAdapter.class)
    static class WithStub {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(WithStub.class);

    @Test
    void isActiveWhenTheProviderIsStub() {
        runner.withPropertyValues("ledgerai.advisor.provider=stub")
                .run(context -> assertThat(context).hasSingleBean(AiAdvisorPort.class)
                        .getBean(AiAdvisorPort.class).isInstanceOf(StubAdvisorAdapter.class));
    }

    @Test
    void isInactiveForAnyOtherProviderSoThatOnlyOneAdvisorExists() {
        runner.withPropertyValues("ledgerai.advisor.provider=anthropic")
                .run(context -> assertThat(context).doesNotHaveBean(AiAdvisorPort.class));
        runner.run(context -> assertThat(context).doesNotHaveBean(AiAdvisorPort.class));
    }
}

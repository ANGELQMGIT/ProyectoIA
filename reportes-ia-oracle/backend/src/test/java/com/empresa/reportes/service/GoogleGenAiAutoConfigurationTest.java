package com.empresa.reportes.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GoogleGenAiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GoogleGenAiChatAutoConfiguration.class))
            .withBean(ToolCallingManager.class, () -> mock(ToolCallingManager.class))
            .withBean(RetryTemplate.class, RetryTemplate::new);

    @Test
    void configuresSingleChatModelWithApiKeyAndModelProperties() {
        contextRunner
                .withPropertyValues(
                        "spring.ai.google.genai.api-key=test-only-key",
                        "spring.ai.google.genai.chat.options.model=gemini-2.5-flash"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatModel.class);
                        assertThat(context.getBean(ChatModel.class).getDefaultOptions().getModel())
                            .isEqualTo("gemini-2.5-flash");
                });
    }

    @Test
    void doesNotConfigureChatModelWhenApiKeyIsMissing() {
        contextRunner
                .withPropertyValues("spring.ai.google.genai.chat.model=gemini-2.5-flash")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("Incomplete Google GenAI configuration: Provide 'api-key' for Gemini API or 'project-id' and 'location' for Vertex AI.");
                });
    }
}
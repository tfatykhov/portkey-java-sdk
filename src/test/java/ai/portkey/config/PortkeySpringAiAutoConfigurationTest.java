package ai.portkey.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PortkeySpringAiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PortkeyAutoConfiguration.class,
                    PortkeySpringAiAutoConfiguration.class
            ));

    @Test
    void activatesWhenApiKeyIsSet() {
        contextRunner
                .withPropertyValues("portkey.api-key=pk-test-123")
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAiApi.class);
                    assertThat(context).hasSingleBean(OpenAiChatModel.class);
                    assertThat(context).hasSingleBean(ChatClient.Builder.class);
                    assertThat(context).hasSingleBean(PortkeyHeaderInterceptor.class);
                });
    }

    @Test
    void doesNotActivateWithoutApiKey() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OpenAiApi.class);
                    assertThat(context).doesNotHaveBean(OpenAiChatModel.class);
                    assertThat(context).doesNotHaveBean(ChatClient.Builder.class);
                });
    }

    @Test
    void coexistsWithPortkeyClient() {
        contextRunner
                .withPropertyValues("portkey.api-key=pk-test-123")
                .run(context -> {
                    // Both auto-configs should produce their beans
                    assertThat(context).hasSingleBean(ai.portkey.client.PortkeyClient.class);
                    assertThat(context).hasSingleBean(OpenAiChatModel.class);
                    assertThat(context).hasSingleBean(ChatClient.Builder.class);
                });
    }

    @Test
    void respectsExistingOpenAiApiBeann() {
        contextRunner
                .withPropertyValues("portkey.api-key=pk-test-123")
                .withBean("customOpenAiApi", OpenAiApi.class,
                        () -> OpenAiApi.builder()
                                .baseUrl("https://custom.example.com")
                                .apiKey("custom-key")
                                .build())
                .run(context -> {
                    // Should use the user-provided bean, not create a new one
                    assertThat(context).hasSingleBean(OpenAiApi.class);
                    assertThat(context.getBean(OpenAiApi.class)).isNotNull();
                });
    }

    @Test
    void configuresWithVirtualKey() {
        contextRunner
                .withPropertyValues(
                        "portkey.api-key=pk-test-123",
                        "portkey.virtual-key=my-openai-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PortkeyHeaderInterceptor.class);
                    assertThat(context).hasSingleBean(OpenAiChatModel.class);
                });
    }

    @Test
    void usesProviderAuthTokenAsApiKey() {
        contextRunner
                .withPropertyValues(
                        "portkey.api-key=pk-test-123",
                        "portkey.provider-auth-token=sk-provider-token"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAiApi.class);
                });
    }

    @Test
    void usesCustomBaseUrl() {
        contextRunner
                .withPropertyValues(
                        "portkey.api-key=pk-test-123",
                        "portkey.base-url=http://my-gateway:8080/v1"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAiApi.class);
                });
    }
}

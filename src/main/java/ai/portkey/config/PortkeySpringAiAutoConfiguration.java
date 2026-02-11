package ai.portkey.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Spring AI auto-configuration for the Portkey gateway.
 *
 * <p>Activates when both conditions are met:
 * <ul>
 *   <li>{@code portkey.api-key} is set</li>
 *   <li>Spring AI's {@link OpenAiChatModel} is on the classpath</li>
 * </ul>
 *
 * <p>Creates an {@link OpenAiChatModel} bean backed by Portkey's OpenAI-compatible API,
 * with all Portkey headers injected automatically. Users get full Spring AI functionality
 * including {@link ChatClient}, {@code @Tool} calling, streaming, and entity mapping.
 *
 * <pre>{@code
 * // application.yml
 * portkey:
 *   api-key: pk-...
 *   virtual-key: my-openai-key
 *
 * // Just inject and use
 * @Service
 * public class MyService {
 *     private final ChatClient chatClient;
 *
 *     public MyService(ChatClient.Builder builder) {
 *         this.chatClient = builder.build();
 *     }
 *
 *     public String chat(String prompt) {
 *         return chatClient.prompt()
 *             .user(prompt)
 *             .tools(new MyTools())
 *             .call()
 *             .content();
 *     }
 * }
 * }</pre>
 */
@AutoConfiguration
@EnableConfigurationProperties(PortkeyProperties.class)
@ConditionalOnClass(OpenAiChatModel.class)
@ConditionalOnProperty(prefix = "portkey", name = "api-key")
public class PortkeySpringAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "portkeyHeaderInterceptor")
    public PortkeyHeaderInterceptor portkeyHeaderInterceptor(PortkeyProperties props) {
        return new PortkeyHeaderInterceptor(props);
    }

    @Bean
    @ConditionalOnMissingBean(OpenAiApi.class)
    public OpenAiApi portkeyOpenAiApi(PortkeyProperties props,
                                       PortkeyHeaderInterceptor headerInterceptor) {
        // Build a RestClient.Builder with Portkey headers interceptor
        var restClientBuilder = RestClient.builder()
                .requestInterceptors(interceptors -> interceptors.add(headerInterceptor));

        // Use provider auth token as the OpenAI API key (Bearer token),
        // or fall back to portkey api-key (Portkey handles auth routing)
        String apiKey = props.getProviderAuthToken() != null
                ? props.getProviderAuthToken()
                : props.getApiKey();

        return OpenAiApi.builder()
                .baseUrl(props.getBaseUrl())
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(OpenAiChatModel.class)
    public OpenAiChatModel portkeyOpenAiChatModel(OpenAiApi openAiApi) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatClient.Builder.class)
    public ChatClient.Builder portkeyChatClientBuilder(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel);
    }
}

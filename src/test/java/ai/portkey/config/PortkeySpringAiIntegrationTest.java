package ai.portkey.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test verifying that Spring AI's ChatClient routes
 * chat completions through the Portkey gateway with correct headers.
 */
@SpringBootTest(
        classes = PortkeySpringAiIntegrationTest.TestApp.class,
        properties = {
                // Prevent Spring AI's own auto-config from creating a competing OpenAiApi bean
                "spring.ai.openai.api-key=portkey-managed"
        }
)
class PortkeySpringAiIntegrationTest {

    // Exclude Spring AI's own OpenAI auto-configs so only our Portkey auto-config runs
    @SpringBootApplication(excludeName = {
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
            // Older package location (pre-1.1.x)
            "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration"
    })
    static class TestApp {}

    static HttpServer mockServer;
    static int port;
    static final CopyOnWriteArrayList<RecordedRequest> recordedRequests = new CopyOnWriteArrayList<>();

    record RecordedRequest(String method, String path, String body,
                           java.util.Map<String, String> headers) {}

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();

        mockServer.createContext("/v1/chat/completions", exchange -> {
            // Record the incoming request
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var headers = new java.util.HashMap<String, String>();
            exchange.getRequestHeaders().forEach((key, values) ->
                    headers.put(key.toLowerCase(), values.getFirst()));
            recordedRequests.add(new RecordedRequest(
                    exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body, headers));

            // Return a valid OpenAI chat completion response
            String response = """
                    {
                      "id": "chatcmpl-springai-test",
                      "object": "chat.completion",
                      "created": 1700000000,
                      "model": "gpt-4o",
                      "choices": [{
                        "index": 0,
                        "finish_reason": "stop",
                        "message": {
                          "role": "assistant",
                          "content": "Hello from Portkey!"
                        },
                        "logprobs": null
                      }],
                      "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 5,
                        "total_tokens": 15
                      }
                    }
                    """;
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        mockServer.stop(0);
    }

    @DynamicPropertySource
    static void configurePortkey(DynamicPropertyRegistry registry) {
        registry.add("portkey.api-key", () -> "pk-test-integration");
        registry.add("portkey.virtual-key", () -> "vk-openai-test");
        registry.add("portkey.provider", () -> "openai");
        registry.add("portkey.config", () -> "cfg-routing-123");
        // Spring AI appends /v1/chat/completions to base-url, so no /v1 suffix here
        registry.add("portkey.base-url", () -> "http://localhost:" + port);
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    void contextLoadsWithAllBeans() {
        assertThat(context.getBean(OpenAiApi.class)).isNotNull();
        assertThat(context.getBean(OpenAiChatModel.class)).isNotNull();
        assertThat(context.getBean(ChatClient.Builder.class)).isNotNull();
        assertThat(context.getBean(PortkeyHeaderInterceptor.class)).isNotNull();
        // Low-level client also available
        assertThat(context.getBean(ai.portkey.client.PortkeyClient.class)).isNotNull();
    }

    @Test
    void chatCompletionThroughSpringAi() {
        recordedRequests.clear();
        var chatClient = chatClientBuilder.build();

        String result = chatClient.prompt()
                .user("What is Portkey?")
                .call()
                .content();

        assertThat(result).isEqualTo("Hello from Portkey!");

        // Verify the request was made with correct Portkey headers
        assertThat(recordedRequests).hasSize(1);
        var recorded = recordedRequests.getFirst();
        assertThat(recorded.method()).isEqualTo("POST");
        assertThat(recorded.path()).isEqualTo("/v1/chat/completions");

        // Verify Portkey headers were injected
        assertThat(recorded.headers().get("x-portkey-api-key")).isEqualTo("pk-test-integration");
        assertThat(recorded.headers().get("x-portkey-virtual-key")).isEqualTo("vk-openai-test");
        assertThat(recorded.headers().get("x-portkey-provider")).isEqualTo("openai");
        assertThat(recorded.headers().get("x-portkey-config")).isEqualTo("cfg-routing-123");

        // Verify request body contains the user message
        assertThat(recorded.body()).contains("What is Portkey?");
    }

    @Test
    void chatCompletionWithSystemPrompt() {
        recordedRequests.clear();
        var chatClient = chatClientBuilder.build();

        String result = chatClient.prompt()
                .system("You are a helpful assistant that works through Portkey.")
                .user("Summarize yourself in one sentence.")
                .call()
                .content();

        assertThat(result).isEqualTo("Hello from Portkey!");

        assertThat(recordedRequests).hasSize(1);
        var body = recordedRequests.getFirst().body();
        assertThat(body).contains("You are a helpful assistant");
        assertThat(body).contains("Summarize yourself");
    }

    @Test
    void entityMappingThroughSpringAi() {
        // Override the mock response for this test to return valid JSON
        // so entity() can parse it
        mockServer.removeContext("/v1/chat/completions");
        mockServer.createContext("/v1/chat/completions", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var headers = new java.util.HashMap<String, String>();
            exchange.getRequestHeaders().forEach((key, values) ->
                    headers.put(key.toLowerCase(), values.getFirst()));
            recordedRequests.add(new RecordedRequest(
                    exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body, headers));

            // Return JSON content that entity() can parse
            String response = """
                    {
                      "id": "chatcmpl-entity-test",
                      "object": "chat.completion",
                      "created": 1700000000,
                      "model": "gpt-4o",
                      "choices": [{
                        "index": 0,
                        "finish_reason": "stop",
                        "message": {
                          "role": "assistant",
                          "content": "{\\"greeting\\": \\"Hello from Portkey!\\"}"
                        },
                        "logprobs": null
                      }],
                      "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 5,
                        "total_tokens": 15
                      }
                    }
                    """;
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });

        recordedRequests.clear();
        var chatClient = chatClientBuilder.build();

        record Greeting(String greeting) {}
        Greeting entity = chatClient.prompt()
                .user("Return a greeting as JSON")
                .call()
                .entity(Greeting.class);

        assertThat(entity).isNotNull();
        assertThat(entity.greeting()).isEqualTo("Hello from Portkey!");
        assertThat(recordedRequests).hasSize(1);

        // Restore the default handler for other tests
        mockServer.removeContext("/v1/chat/completions");
        mockServer.createContext("/v1/chat/completions", exchange -> {
            var body2 = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var headers2 = new java.util.HashMap<String, String>();
            exchange.getRequestHeaders().forEach((key, values) ->
                    headers2.put(key.toLowerCase(), values.getFirst()));
            recordedRequests.add(new RecordedRequest(
                    exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body2, headers2));

            String defaultResponse = """
                    {
                      "id": "chatcmpl-springai-test",
                      "object": "chat.completion",
                      "created": 1700000000,
                      "model": "gpt-4o",
                      "choices": [{
                        "index": 0,
                        "finish_reason": "stop",
                        "message": {
                          "role": "assistant",
                          "content": "Hello from Portkey!"
                        },
                        "logprobs": null
                      }],
                      "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 5,
                        "total_tokens": 15
                      }
                    }
                    """;
            byte[] bytes = defaultResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }
}

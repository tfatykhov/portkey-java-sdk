package ai.portkey.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full integration test verifying that Spring AI's ChatClient routes
 * chat completions through the Portkey gateway with correct headers.
 *
 * <p>Covers all usage scenarios:
 * <ul>
 *   <li>Simple chat completion</li>
 *   <li>System prompt + user message</li>
 *   <li>Entity mapping (JSON response → Java record)</li>
 *   <li>Tool calling (@Tool dispatch loop)</li>
 *   <li>Portkey header injection verification</li>
 *   <li>Bean coexistence (PortkeyClient + ChatClient)</li>
 * </ul>
 */
@SpringBootTest(
        classes = PortkeySpringAiIntegrationTest.TestApp.class,
        properties = "spring.ai.openai.api-key=portkey-managed"
)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortkeySpringAiIntegrationTest {

    @SpringBootApplication(excludeName = {
            "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration",
            "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration",
            "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration"
    })
    static class TestApp {}

    // -- Mock server infrastructure --

    static HttpServer mockServer;
    static int port;
    static final CopyOnWriteArrayList<RecordedRequest> recordedRequests = new CopyOnWriteArrayList<>();

    /**
     * Queue of responses the mock server will return in order.
     * When empty, falls back to the default text response.
     */
    static final LinkedBlockingDeque<String> responseQueue = new LinkedBlockingDeque<>();

    record RecordedRequest(String method, String path, String body,
                           java.util.Map<String, String> headers) {}

    // -- Tool definitions for tool-calling test --

    static class WeatherTools {
        @Tool(description = "Get current weather for a city")
        public String getWeather(@ToolParam(description = "City name") String city) {
            return "{\"city\": \"" + city + "\", \"temp\": \"22C\", \"condition\": \"sunny\"}";
        }
    }

    static class CalculatorTools {
        @Tool(description = "Add two numbers together")
        public int add(@ToolParam(description = "First number") int a,
                       @ToolParam(description = "Second number") int b) {
            return a + b;
        }
    }

    // -- Server lifecycle --

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        port = mockServer.getAddress().getPort();

        mockServer.createContext("/v1/chat/completions", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var headers = new java.util.HashMap<String, String>();
            exchange.getRequestHeaders().forEach((key, values) ->
                    headers.put(key.toLowerCase(), values.getFirst()));
            recordedRequests.add(new RecordedRequest(
                    exchange.getRequestMethod(), exchange.getRequestURI().getPath(), body, headers));

            // Use queued response if available, otherwise default
            String response = responseQueue.poll();
            if (response == null) {
                response = defaultTextResponse("Hello from Portkey!");
            }

            sendJson(exchange, response);
        });

        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        mockServer.stop(0);
    }

    @BeforeEach
    void clearState() {
        recordedRequests.clear();
        responseQueue.clear();
    }

    @DynamicPropertySource
    static void configurePortkey(DynamicPropertyRegistry registry) {
        registry.add("portkey.api-key", () -> "pk-test-integration");
        registry.add("portkey.virtual-key", () -> "vk-openai-test");
        registry.add("portkey.provider", () -> "openai");
        registry.add("portkey.config", () -> "cfg-routing-123");
        registry.add("portkey.base-url", () -> "http://localhost:" + port);
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    // ============================
    // 1. Context & Bean Wiring
    // ============================

    @Test
    @Order(1)
    void contextLoadsWithAllBeans() {
        assertThat(context.getBean(OpenAiApi.class)).isNotNull();
        assertThat(context.getBean(OpenAiChatModel.class)).isNotNull();
        assertThat(context.getBean(ChatClient.Builder.class)).isNotNull();
        assertThat(context.getBean(PortkeyHeaderInterceptor.class)).isNotNull();
        assertThat(context.getBean(ai.portkey.client.PortkeyClient.class)).isNotNull();
    }

    // ============================
    // 2. Simple Chat Completion
    // ============================

    @Test
    @Order(2)
    void simpleChatCompletion() {
        var chatClient = chatClientBuilder.build();

        String result = chatClient.prompt()
                .user("What is Portkey?")
                .call()
                .content();

        assertThat(result).isEqualTo("Hello from Portkey!");

        assertThat(recordedRequests).hasSize(1);
        var req = recordedRequests.getFirst();
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.path()).isEqualTo("/v1/chat/completions");
        assertThat(req.body()).contains("What is Portkey?");
    }

    // ============================
    // 3. Portkey Headers Injected
    // ============================

    @Test
    @Order(3)
    void portkeyHeadersInjected() {
        var chatClient = chatClientBuilder.build();
        chatClient.prompt().user("test").call().content();

        assertThat(recordedRequests).hasSize(1);
        var headers = recordedRequests.getFirst().headers();

        assertThat(headers.get("x-portkey-api-key")).isEqualTo("pk-test-integration");
        assertThat(headers.get("x-portkey-virtual-key")).isEqualTo("vk-openai-test");
        assertThat(headers.get("x-portkey-provider")).isEqualTo("openai");
        assertThat(headers.get("x-portkey-config")).isEqualTo("cfg-routing-123");
        // Bearer token should also be present (Spring AI sends Authorization header)
        assertThat(headers.get("authorization")).isNotNull();
    }

    // ============================
    // 4. System + User Prompt
    // ============================

    @Test
    @Order(4)
    void systemAndUserPrompt() {
        responseQueue.add(defaultTextResponse("I am an AI assistant routed through Portkey."));

        var chatClient = chatClientBuilder.build();

        String result = chatClient.prompt()
                .system("You are a helpful assistant that works through Portkey.")
                .user("Summarize yourself in one sentence.")
                .call()
                .content();

        assertThat(result).isEqualTo("I am an AI assistant routed through Portkey.");

        assertThat(recordedRequests).hasSize(1);
        var body = recordedRequests.getFirst().body();
        assertThat(body).contains("You are a helpful assistant");
        assertThat(body).contains("Summarize yourself");
        // Should contain both system and user roles
        assertThat(body).contains("\"role\"");
    }

    // ============================
    // 5. Entity Mapping
    // ============================

    record MovieRec(String title, int year, String reason) {}

    @Test
    @Order(5)
    void entityMapping() {
        responseQueue.add(defaultTextResponse(
                "{\"title\": \"Blade Runner 2049\", \"year\": 2017, \"reason\": \"Stunning visuals and deep themes\"}"));

        var chatClient = chatClientBuilder.build();

        MovieRec movie = chatClient.prompt()
                .user("Recommend a sci-fi movie, respond in JSON")
                .call()
                .entity(MovieRec.class);

        assertThat(movie).isNotNull();
        assertThat(movie.title()).isEqualTo("Blade Runner 2049");
        assertThat(movie.year()).isEqualTo(2017);
        assertThat(movie.reason()).isEqualTo("Stunning visuals and deep themes");
    }

    record Greeting(String greeting, String language) {}

    @Test
    @Order(6)
    void entityMappingMultipleFields() {
        responseQueue.add(defaultTextResponse(
                "{\"greeting\": \"Bonjour\", \"language\": \"French\"}"));

        var chatClient = chatClientBuilder.build();

        Greeting greeting = chatClient.prompt()
                .user("Greet me in French")
                .call()
                .entity(Greeting.class);

        assertThat(greeting.greeting()).isEqualTo("Bonjour");
        assertThat(greeting.language()).isEqualTo("French");
    }

    // ============================
    // 6. Tool Calling (Weather)
    // ============================

    @Test
    @Order(7)
    void toolCallingWeather() {
        // First response: model requests the getWeather tool
        responseQueue.add("""
                {
                  "id": "chatcmpl-tool-1",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "tool_calls",
                    "message": {
                      "role": "assistant",
                      "content": null,
                      "tool_calls": [{
                        "id": "call_weather_001",
                        "type": "function",
                        "function": {
                          "name": "getWeather",
                          "arguments": "{\\"city\\": \\"Tokyo\\"}"
                        }
                      }]
                    },
                    "logprobs": null
                  }],
                  "usage": {"prompt_tokens": 50, "completion_tokens": 20, "total_tokens": 70}
                }
                """);

        // Second response: model uses the tool result to give final answer
        responseQueue.add(defaultTextResponse(
                "The weather in Tokyo is 22C and sunny."));

        var chatClient = chatClientBuilder.build();

        String result = chatClient.prompt()
                .user("What's the weather in Tokyo?")
                .tools(new WeatherTools())
                .call()
                .content();

        assertThat(result).isEqualTo("The weather in Tokyo is 22C and sunny.");

        // Should have 2 requests: initial + tool result follow-up
        assertThat(recordedRequests).hasSize(2);

        // First request: contains user message and tool definitions
        var firstReq = recordedRequests.get(0);
        assertThat(firstReq.body()).contains("What's the weather in Tokyo?");
        assertThat(firstReq.body()).contains("getWeather");
        assertThat(firstReq.body()).contains("tools");

        // Second request: contains tool result
        var secondReq = recordedRequests.get(1);
        assertThat(secondReq.body()).contains("tool");
        assertThat(secondReq.body()).contains("22C");
        assertThat(secondReq.body()).contains("sunny");

        // Both requests should have Portkey headers
        assertThat(secondReq.headers().get("x-portkey-api-key")).isEqualTo("pk-test-integration");
        assertThat(secondReq.headers().get("x-portkey-virtual-key")).isEqualTo("vk-openai-test");
    }

    // ============================
    // 7. Tool Calling (Calculator)
    // ============================

    @Test
    @Order(8)
    void toolCallingCalculator() {
        // First response: model requests the add tool
        responseQueue.add("""
                {
                  "id": "chatcmpl-calc-1",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "tool_calls",
                    "message": {
                      "role": "assistant",
                      "content": null,
                      "tool_calls": [{
                        "id": "call_calc_001",
                        "type": "function",
                        "function": {
                          "name": "add",
                          "arguments": "{\\"a\\": 17, \\"b\\": 25}"
                        }
                      }]
                    },
                    "logprobs": null
                  }],
                  "usage": {"prompt_tokens": 30, "completion_tokens": 15, "total_tokens": 45}
                }
                """);

        // Second response: final answer
        responseQueue.add(defaultTextResponse("17 + 25 = 42"));

        var chatClient = chatClientBuilder.build();

        String result = chatClient.prompt()
                .user("What is 17 + 25?")
                .tools(new CalculatorTools())
                .call()
                .content();

        assertThat(result).isEqualTo("17 + 25 = 42");

        assertThat(recordedRequests).hasSize(2);

        // Second request should contain the tool result "42"
        var secondReq = recordedRequests.get(1);
        assertThat(secondReq.body()).contains("42");
    }

    // ============================
    // 8. Multiple Requests Reuse Connection
    // ============================

    @Test
    @Order(9)
    void multipleSequentialRequests() {
        responseQueue.add(defaultTextResponse("First response"));
        responseQueue.add(defaultTextResponse("Second response"));

        var chatClient = chatClientBuilder.build();

        String first = chatClient.prompt().user("First question").call().content();
        String second = chatClient.prompt().user("Second question").call().content();

        assertThat(first).isEqualTo("First response");
        assertThat(second).isEqualTo("Second response");
        assertThat(recordedRequests).hasSize(2);

        // Both requests should have Portkey headers
        for (var req : recordedRequests) {
            assertThat(req.headers().get("x-portkey-api-key")).isEqualTo("pk-test-integration");
        }
    }

    // ============================
    // 9. Coexistence: Both Clients Work
    // ============================

    @Test
    @Order(10)
    void bothClientsCoexist() {
        // ChatClient (Spring AI) works
        var chatClient = chatClientBuilder.build();
        String result = chatClient.prompt().user("hi").call().content();
        assertThat(result).isNotNull();

        // PortkeyClient (low-level) is also wired
        var portkeyClient = context.getBean(ai.portkey.client.PortkeyClient.class);
        assertThat(portkeyClient).isNotNull();
    }

    // -- Helpers --

    private static String defaultTextResponse(String content) {
        // Escape JSON special chars in content
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "stop",
                    "message": {
                      "role": "assistant",
                      "content": "%s"
                    },
                    "logprobs": null
                  }],
                  "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
                }
                """.formatted(escaped);
    }

    private static void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

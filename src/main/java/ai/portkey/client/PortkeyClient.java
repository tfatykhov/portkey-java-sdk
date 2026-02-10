package ai.portkey.client;

import ai.portkey.exception.PortkeyException;
import ai.portkey.model.ChatCompletionRequest;
import ai.portkey.model.ChatCompletionResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for the Portkey AI Gateway.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * PortkeyClient client = PortkeyClient.builder()
 *     .apiKey("pk-...")
 *     .virtualKey("my-openai-key")
 *     .build();
 *
 * ChatCompletionResponse resp = client.chatCompletions().create(
 *     ChatCompletionRequest.builder()
 *         .model("gpt-4o")
 *         .addMessage(Message.user("Hello!"))
 *         .build()
 * );
 *
 * System.out.println(resp.getContent());
 * }</pre>
 */
public class PortkeyClient {

    private static final String DEFAULT_BASE_URL = "https://api.portkey.ai/v1";

    private final String baseUrl;
    private final Map<String, String> defaultHeaders;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    private final ChatCompletions chatCompletions;

    private PortkeyClient(Builder builder) {
        this.baseUrl = builder.baseUrl != null ? builder.baseUrl : DEFAULT_BASE_URL;
        this.defaultHeaders = new LinkedHashMap<>(builder.headers);
        this.timeout = builder.timeout;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        this.objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.chatCompletions = new ChatCompletions();
    }

    /**
     * Access the chat completions API.
     */
    public ChatCompletions chatCompletions() {
        return chatCompletions;
    }

    // -- inner resource class --

    public class ChatCompletions {

        /**
         * Create a chat completion.
         *
         * @param request the completion request
         * @return parsed response
         * @throws PortkeyException on API errors or network failures
         */
        public ChatCompletionResponse create(ChatCompletionRequest request) {
            return post("/chat/completions", request, ChatCompletionResponse.class);
        }
    }

    // -- HTTP layer --

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            String json = objectMapper.writeValueAsString(body);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));

            defaultHeaders.forEach(reqBuilder::header);

            HttpResponse<String> response = httpClient.send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PortkeyException(response.statusCode(), response.body());
            }

            return objectMapper.readValue(response.body(), responseType);
        } catch (PortkeyException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new PortkeyException("Request failed: " + e.getMessage(), e);
        }
    }

    // -- builder --

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private Duration timeout = Duration.ofSeconds(60);
        private final Map<String, String> headers = new LinkedHashMap<>();

        /**
         * Portkey API key (required). Sent as {@code x-portkey-api-key}.
         */
        public Builder apiKey(String apiKey) {
            headers.put("x-portkey-api-key", apiKey);
            return this;
        }

        /**
         * Virtual key for provider routing. Sent as {@code x-portkey-virtual-key}.
         */
        public Builder virtualKey(String virtualKey) {
            headers.put("x-portkey-virtual-key", virtualKey);
            return this;
        }

        /**
         * Provider name for direct auth. Sent as {@code x-portkey-provider}.
         */
        public Builder provider(String provider) {
            headers.put("x-portkey-provider", provider);
            return this;
        }

        /**
         * Provider auth token (Bearer). Sent as {@code Authorization: Bearer ...}.
         */
        public Builder providerAuth(String token) {
            headers.put("Authorization", "Bearer " + token);
            return this;
        }

        /**
         * Portkey config ID. Sent as {@code x-portkey-config}.
         */
        public Builder config(String configId) {
            headers.put("x-portkey-config", configId);
            return this;
        }

        /**
         * Custom host override. Sent as {@code x-portkey-custom-host}.
         */
        public Builder customHost(String host) {
            headers.put("x-portkey-custom-host", host);
            return this;
        }

        /**
         * Trace ID for observability. Sent as {@code x-portkey-trace-id}.
         */
        public Builder traceId(String traceId) {
            headers.put("x-portkey-trace-id", traceId);
            return this;
        }

        /**
         * Metadata for logging. Sent as JSON in {@code x-portkey-metadata}.
         */
        public Builder metadata(String metadataJson) {
            headers.put("x-portkey-metadata", metadataJson);
            return this;
        }

        /**
         * Cache namespace. Sent as {@code x-portkey-cache-namespace}.
         */
        public Builder cacheNamespace(String ns) {
            headers.put("x-portkey-cache-namespace", ns);
            return this;
        }

        /**
         * Force cache refresh. Sent as {@code x-portkey-cache-force-refresh}.
         */
        public Builder cacheForceRefresh(boolean force) {
            headers.put("x-portkey-cache-force-refresh", String.valueOf(force));
            return this;
        }

        /**
         * Add an arbitrary header.
         */
        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Override the base URL (default: {@code https://api.portkey.ai/v1}).
         * Use for self-hosted gateways.
         */
        public Builder baseUrl(String baseUrl) {
            // strip trailing slash
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            return this;
        }

        /**
         * HTTP timeout (default: 60s).
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public PortkeyClient build() {
            if (!headers.containsKey("x-portkey-api-key")) {
                throw new IllegalStateException("apiKey is required");
            }
            return new PortkeyClient(this);
        }
    }
}

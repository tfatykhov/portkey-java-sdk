package ai.portkey.client;

import ai.portkey.exception.PortkeyException;
import ai.portkey.model.ChatCompletionRequest;
import ai.portkey.model.ChatCompletionResponse;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client for the Portkey AI Gateway, backed by Spring's {@link RestClient}.
 *
 * <h2>Spring Boot (auto-configured)</h2>
 * <pre>{@code
 * // application.yml
 * portkey:
 *   api-key: pk-...
 *   virtual-key: my-openai-key
 *
 * // Just inject
 * @Service
 * public class MyService {
 *     private final PortkeyClient portkey;
 *
 *     public MyService(PortkeyClient portkey) {
 *         this.portkey = portkey;
 *     }
 *
 *     public String chat(String prompt) {
 *         var resp = portkey.chatCompletions().create(
 *             ChatCompletionRequest.builder()
 *                 .model("gpt-4o")
 *                 .addMessage(Message.user(prompt))
 *                 .build()
 *         );
 *         return resp.getContent();
 *     }
 * }
 * }</pre>
 *
 * <h2>Standalone (no Spring Boot)</h2>
 * <pre>{@code
 * var restClient = RestClient.builder()
 *     .baseUrl("https://api.portkey.ai/v1")
 *     .defaultHeader("x-portkey-api-key", "pk-...")
 *     .defaultHeader("x-portkey-virtual-key", "my-key")
 *     .build();
 * var client = new PortkeyClient(restClient);
 * }</pre>
 */
public class PortkeyClient {

    private final RestClient restClient;
    private final ChatCompletions chatCompletions;

    public PortkeyClient(RestClient restClient) {
        this.restClient = restClient;
        this.chatCompletions = new ChatCompletions();
    }

    /**
     * Access the chat completions API.
     */
    public ChatCompletions chatCompletions() {
        return chatCompletions;
    }

    /**
     * Chat completions resource.
     */
    public class ChatCompletions {

        /**
         * Create a chat completion.
         *
         * @param request the completion request
         * @return parsed response
         * @throws PortkeyException on API errors
         */
        public ChatCompletionResponse create(ChatCompletionRequest request) {
            return create(request, null);
        }

        /**
         * Create a chat completion with per-request headers.
         *
         * @param request the completion request
         * @param requestCustomizer customize headers per-request (trace ID, metadata, etc.)
         * @return parsed response
         * @throws PortkeyException on API errors
         */
        public ChatCompletionResponse create(ChatCompletionRequest request,
                                              RequestCustomizer requestCustomizer) {
            try {
                var spec = restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request);

                if (requestCustomizer != null) {
                    requestCustomizer.customize(spec);
                }

                return spec.retrieve().body(ChatCompletionResponse.class);
            } catch (RestClientResponseException e) {
                throw new PortkeyException(e.getStatusCode().value(), e.getResponseBodyAsString());
            } catch (Exception e) {
                throw new PortkeyException("Request failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Functional interface for per-request customization (e.g., trace IDs, metadata).
     *
     * <pre>{@code
     * client.chatCompletions().create(request, spec ->
     *     spec.header("x-portkey-trace-id", "my-trace-123")
     *         .header("x-portkey-metadata", "{\"user\":\"abc\"}")
     * );
     * }</pre>
     */
    @FunctionalInterface
    public interface RequestCustomizer {
        void customize(RestClient.RequestBodySpec spec);
    }
}

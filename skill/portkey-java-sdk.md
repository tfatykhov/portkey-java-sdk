# Skill: Using the Portkey Java SDK

You are helping a developer integrate the **Portkey Java SDK** into their Spring Boot application. This SDK provides type-safe Java access to the [Portkey AI Gateway](https://portkey.ai), which offers unified access to multiple AI providers (OpenAI, Anthropic Claude, etc.) with intelligent routing, caching, and observability.

## When to Use This Skill

Use this skill when the developer needs to:
- Add AI/LLM capabilities to a Spring Boot application via Portkey
- Send chat completion requests to any AI provider through Portkey's gateway
- Work with multimodal content (text + images + files)
- Implement tool calling (function calling) with LLMs
- Configure Portkey routing, caching, or observability headers
- Use Spring AI's `ChatClient` with Portkey as the gateway

## Library Overview

The SDK offers two levels of API:

| API | Use Case |
|-----|----------|
| **`PortkeyClient`** | Lightweight REST client with per-request header control. Best for direct API access. |
| **Spring AI `ChatClient`** | Full-featured with `@Tool` calling, streaming, entity mapping. Requires Spring AI on classpath. |

## Requirements

- Java 25+
- Spring Boot 3.5+
- Spring AI 1.1.2+ (optional, for `ChatClient`)

## Step 1: Add Dependency

### Gradle

```groovy
implementation 'ai.portkey:portkey-java-sdk:0.1.0-SNAPSHOT'
```

### Maven

```xml
<dependency>
    <groupId>ai.portkey</groupId>
    <artifactId>portkey-java-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For Spring AI integration, also add:

```groovy
implementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.2'
```

## Step 2: Configure

Add to `application.yml`:

```yaml
portkey:
  api-key: pk-...                        # Required - Portkey API key
  virtual-key: my-openai-virtual-key     # Provider virtual key
```

### All Configuration Properties

```yaml
portkey:
  api-key: pk-...                        # Required
  virtual-key: my-key                    # Provider virtual key
  provider: openai                       # Provider name (for direct auth)
  provider-auth-token: sk-...            # Provider Bearer token
  config: my-config-id                   # Config ID for routing/fallbacks
  custom-host: https://my-proxy.com      # Custom provider host
  base-url: https://api.portkey.ai/v1    # Override base URL (default shown)
  timeout: 60s                           # HTTP timeout (default: 60s)
  headers:                               # Extra headers on every request
    x-portkey-trace-id: my-trace
```

The SDK auto-configures a `PortkeyClient` bean when `portkey.api-key` is present. No additional `@Bean` definitions needed.

## Step 3: Use the Client

### Basic Chat Completion

```java
import ai.portkey.client.PortkeyClient;
import ai.portkey.model.ChatCompletionRequest;
import ai.portkey.model.ChatCompletionResponse;
import ai.portkey.model.Message;

@Service
public class ChatService {
    private final PortkeyClient portkey;

    public ChatService(PortkeyClient portkey) {
        this.portkey = portkey;
    }

    public String chat(String prompt) {
        ChatCompletionResponse response = portkey.chatCompletions().create(
            ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.system("You are a helpful assistant."))
                .addMessage(Message.user(prompt))
                .temperature(0.7)
                .maxTokens(500)
                .build()
        );
        return response.getContent();
    }
}
```

### Key Rule: `response.getContent()` returns the first choice's text content.

## Important Patterns to Know

### Message Factory Methods

Always use the static factory methods on `Message`. Never call constructors directly.

```java
Message.system("You are helpful.")              // System instructions
Message.developer("Coding assistant rules")     // Developer role (OpenAI)
Message.user("Hello!")                           // User text
Message.user(List.of(contentParts...))           // User multimodal
Message.assistant("Hi there!")                   // Assistant response
Message.assistant(null, toolCalls)               // Assistant with tool calls
Message.tool(result, callId)                     // Tool response

// Named participants
Message.user("Hello!").withName("alice")
```

### Request Builder

Always use the builder pattern. `model` and at least one message are required.

```java
ChatCompletionRequest.builder()
    .model("gpt-4o")                              // Required
    .addMessage(Message.user("Hello"))             // Required (at least one)
    .temperature(0.7)                              // Optional: 0.0-2.0
    .topP(0.9)                                     // Optional: nucleus sampling
    .maxTokens(500)                                // Optional: max completion tokens
    .n(1)                                          // Optional: number of choices
    .stop("\\n")                                   // Optional: stop sequence(s)
    .presencePenalty(0.0)                           // Optional: -2.0 to 2.0
    .frequencyPenalty(0.0)                          // Optional: -2.0 to 2.0
    .seed(42)                                      // Optional: deterministic sampling
    .responseFormat(Map.of("type", "json_object")) // Optional: JSON mode
    .user("user-123")                              // Optional: end-user ID
    .tools(toolDefinitions)                        // Optional: function tools
    .toolChoice("auto")                            // Optional: tool strategy
    .parallelToolCalls(true)                       // Optional: parallel calls
    .thinking(Map.of("type", "enabled",
        "budget_tokens", 4096))                    // Optional: Claude thinking
    .build()
```

### Error Handling

```java
import ai.portkey.exception.PortkeyException;

try {
    var response = portkey.chatCompletions().create(request);
} catch (PortkeyException e) {
    int status = e.getStatusCode();        // HTTP status, -1 for transport errors
    String msg = e.getErrorMessage();      // Parsed error.message
    String type = e.getErrorType();        // e.g. "auth_error", "rate_limit_error"
    String code = e.getErrorCode();        // Parsed error.code
    String body = e.getResponseBody();     // Raw response body
}
```

### Per-Request Headers

Use the `RequestCustomizer` for trace IDs, metadata, and cache control:

```java
portkey.chatCompletions().create(request, spec ->
    spec.header("x-portkey-trace-id", "trace-123")
        .header("x-portkey-metadata", "{\"user\":\"abc\"}")
        .header("x-portkey-cache-force-refresh", "true")
);
```

### Standalone Usage (Without Spring Boot)

```java
var restClient = RestClient.builder()
    .baseUrl("https://api.portkey.ai/v1")
    .defaultHeader("x-portkey-api-key", "pk-...")
    .defaultHeader("x-portkey-virtual-key", "my-key")
    .build();

var client = new PortkeyClient(restClient);
```

## Common Tasks

Refer to `skill/examples.md` for complete code examples of these tasks:

1. **Basic chat** - Simple text completion
2. **Multimodal/vision** - Images + text
3. **Tool calling** - Function calling with tool loop
4. **Thinking mode** - Claude extended thinking
5. **File input** - PDFs and documents
6. **Image conversion** - Raw bytes to content parts
7. **Spring AI ChatClient** - @Tool, streaming, entity mapping
8. **Retry/resilience** - Spring Retry integration

## Things to Avoid

- **Do NOT use streaming with `PortkeyClient`** - SSE support is not implemented. Use Spring AI `ChatClient` for streaming.
- **Do NOT call `new Message()` directly** - Always use `Message.system()`, `Message.user()`, etc.
- **Do NOT forget `model` or messages** - The builder will throw `IllegalStateException`.
- **Do NOT expect built-in retry** - Add your own resilience layer (Spring Retry, Resilience4j).
- **Do NOT hardcode API keys** - Use `application.yml` or environment variables.

## Package Structure Reference

```
ai.portkey.client.PortkeyClient          - Main client
ai.portkey.model.ChatCompletionRequest   - Request builder
ai.portkey.model.ChatCompletionResponse  - Response record
ai.portkey.model.Message                 - All message roles
ai.portkey.model.ContentPart             - Sealed interface for multimodal
ai.portkey.model.TextContentPart         - Text content
ai.portkey.model.ImageContentPart        - Image content (URL/base64)
ai.portkey.model.FileContentPart         - File content (PDF, text, CSV)
ai.portkey.model.Choice                  - Completion choice
ai.portkey.model.Usage                   - Token usage stats
ai.portkey.model.ToolCall                - Tool call from model
ai.portkey.exception.PortkeyException    - API errors
ai.portkey.utils.ImageUtils              - Image conversion utilities
ai.portkey.config.PortkeyProperties      - Configuration properties
```

# Portkey Java SDK - Usage Guide

Reference document for AI coding agents using the Portkey Java SDK.

## Setup

### Gradle dependency

```groovy
implementation 'ai.portkey:portkey-java-sdk:0.1.0-SNAPSHOT'
```

### Spring Boot configuration

```yaml
portkey:
  api-key: pk-...              # Required - Portkey API key
  virtual-key: my-openai-key   # Provider virtual key
  base-url: https://api.portkey.ai/v1  # Default, override for self-hosted
  timeout: 60s                 # HTTP timeout (Duration format)
```

The SDK auto-configures a `PortkeyClient` bean when `portkey.api-key` is present. Just inject it.

### Standalone (no Spring Boot)

```java
import ai.portkey.client.PortkeyClient;
import org.springframework.web.client.RestClient;

var restClient = RestClient.builder()
    .baseUrl("https://api.portkey.ai/v1")
    .defaultHeader("x-portkey-api-key", "pk-...")
    .defaultHeader("x-portkey-virtual-key", "my-key")
    .build();

var portkey = new PortkeyClient(restClient);
```

---

## Core API

### Imports

```java
import ai.portkey.client.PortkeyClient;
import ai.portkey.model.*;
import ai.portkey.utils.ImageUtils;
import ai.portkey.exception.PortkeyException;
```

### Basic chat completion

```java
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.system("You are a helpful assistant."))
        .addMessage(Message.user("Hello!"))
        .temperature(0.7)
        .maxTokens(500)
        .build()
);

String content = response.getContent();  // First choice's text
```

### Response structure

```java
ChatCompletionResponse response = portkey.chatCompletions().create(request);

response.id();              // "chatcmpl-..."
response.model();           // "gpt-4o-2024-..."
response.choices();         // List<Choice>
response.usage();           // Usage record

// Choice
Choice choice = response.choices().getFirst();
choice.index();             // 0
choice.finishReason();      // "stop", "tool_calls", etc.
choice.message();           // Message

// Usage
response.usage().promptTokens();
response.usage().completionTokens();
response.usage().totalTokens();

// Convenience
response.getContent();      // First choice message text (or null)
```

---

## Messages

### All roles

```java
Message.system("System instructions")
Message.developer("Developer instructions")     // Newer OpenAI role
Message.user("User text")
Message.user(List.of(contentPart1, contentPart2))  // Multimodal
Message.assistant("Assistant response")
Message.assistant(null, toolCallsList)           // With tool calls
Message.tool(resultJson, "call_abc123")          // Tool response
```

### Named participants

```java
Message.user("Hello!").withName("alice")
Message.user("Hi there!").withName("bob")
```

### Reading message content

```java
message.getRole();            // "user", "assistant", etc.
message.getContentAsText();   // String or null (if multimodal)
message.getContentAsParts();  // List<ContentPart> or null (if text)
message.getContent();         // Raw Object (String or List)
message.getToolCalls();       // List<ToolCall> or null
message.getToolCallId();      // String or null (tool messages only)
message.getName();            // String or null
```

---

## Multimodal (Vision)

### Content parts

```java
// Text
ContentPart.text("What's in this image?")

// Image from URL
ContentPart.imageUrl("https://example.com/photo.jpg")
ContentPart.imageUrl("https://example.com/photo.jpg", ImageContentPart.Detail.high)

// Image from base64
ContentPart.imageBase64("image/png", base64String)
ContentPart.imageBase64("image/jpeg", base64String, ImageContentPart.Detail.low)
```

### Detail levels

```java
ImageContentPart.Detail.auto   // Let the model decide
ImageContentPart.Detail.low    // Faster, fewer tokens
ImageContentPart.Detail.high   // More detail, more tokens
```

### Multimodal message

```java
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.user(List.of(
            ContentPart.text("Describe this image"),
            ContentPart.imageUrl("https://example.com/photo.jpg", ImageContentPart.Detail.high)
        )))
        .build()
);
```

---

## ImageUtils

Utility for converting raw image bytes to PNG base64 content parts. Located in `ai.portkey.utils.ImageUtils`.

### Convert image bytes to ContentPart

```java
byte[] photoBytes = Files.readAllBytes(Path.of("photo.jpg"));

// Direct conversion (any format -> PNG base64)
ImageContentPart part = ImageUtils.toPngContentPart(photoBytes);

// With detail level
ImageContentPart partHD = ImageUtils.toPngContentPart(photoBytes, ImageContentPart.Detail.high);

// Just the base64 string
String base64 = ImageUtils.toPngBase64(photoBytes);
```

### Resize before converting

Scales proportionally by the larger dimension. Preserves aspect ratio.

```java
// Resize to max 800px, then convert to ContentPart
ImageContentPart part = ImageUtils.toPngContentPart(photoBytes, 800);

// Resize with detail level
ImageContentPart partLow = ImageUtils.toPngContentPart(photoBytes, 800, ImageContentPart.Detail.low);

// Resize to raw PNG bytes
byte[] resizedPng = ImageUtils.resize(photoBytes, 800);

// Resize + base64
String resizedBase64 = ImageUtils.toPngBase64(photoBytes, 800);
```

**Resize examples:**
| Input | maxSize | Output |
|-------|---------|--------|
| 4000x3000 | 800 | 800x600 |
| 3000x4000 | 800 | 600x800 |
| 1920x1080 | 800 | 800x450 |
| 600x400 | 800 | 600x400 (no change) |

**Supported input formats:** JPEG, PNG, BMP, GIF, TIFF, WBMP (anything `javax.imageio.ImageIO` supports).

**Safety:** Max 8192px per dimension (decompression bomb protection). Throws `IllegalArgumentException` if exceeded.

---

## Tool Calling

### Define tools and send

```java
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.user("What's the weather in NYC?"))
        .tools(List.of(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "get_weather",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of("city", Map.of("type", "string"))
                )
            )
        )))
        .build()
);
```

### Process tool calls

```java
var toolCalls = response.choices().getFirst().message().getToolCalls();
for (var call : toolCalls) {
    call.id();                    // "call_abc123"
    call.type();                  // "function"
    call.function().name();       // "get_weather"
    call.function().arguments();  // "{\"city\":\"NYC\"}"
}
```

### Send tool results back

```java
var followUp = ChatCompletionRequest.builder()
    .model("gpt-4o")
    .addMessage(Message.user("What's the weather in NYC?"))
    .addMessage(Message.assistant(null, toolCalls))
    .addMessage(Message.tool("{\"temp\":\"72F\"}", "call_abc123"))
    .build();

var finalResponse = portkey.chatCompletions().create(followUp);
```

---

## Request Parameters

All parameters on `ChatCompletionRequest.Builder`:

```java
ChatCompletionRequest.builder()
    .model("gpt-4o")                          // Required
    .addMessage(msg)                          // Required (at least one)
    .addMessages(List.of(msg1, msg2))         // Bulk add
    .temperature(0.7)                         // 0.0 - 2.0
    .topP(1.0)                                // Nucleus sampling
    .n(1)                                     // Number of choices
    .stop("END")                              // Stop sequence(s)
    .maxTokens(500)                           // Max completion tokens
    .presencePenalty(0.0)                      // -2.0 to 2.0
    .frequencyPenalty(0.0)                     // -2.0 to 2.0
    .logitBias(Map.of("50256", -100))         // Token bias
    .user("user-123")                         // End-user ID
    .logprobs(true)                           // Return log probabilities
    .topLogprobs(5)                           // Top N logprobs per token
    .responseFormat(Map.of("type", "json_object"))  // Response format
    .seed(42)                                 // Deterministic sampling
    .tools(toolsList)                         // Function tools
    .toolChoice("auto")                       // Tool choice strategy
    .parallelToolCalls(true)                  // Allow parallel calls
    .thinking(Map.of("type", "enabled", "budget_tokens", 4096))  // Claude thinking
    .build();
```

---

## Per-Request Headers

```java
portkey.chatCompletions().create(request, spec -> {
    spec.header("x-portkey-trace-id", "trace-123");
    spec.header("x-portkey-metadata", "{\"user\":\"abc\"}");
    spec.header("x-portkey-cache-force-refresh", "true");
});
```

---

## Error Handling

```java
try {
    var response = portkey.chatCompletions().create(request);
} catch (PortkeyException e) {
    e.getStatusCode();      // HTTP status (401, 429, 500, etc.) or -1 for transport errors
    e.getErrorMessage();    // Parsed error.message (or null)
    e.getErrorType();       // Parsed error.type ("auth_error", "rate_limit_error", etc.)
    e.getErrorCode();       // Parsed error.code (or null)
    e.getResponseBody();    // Raw response body string
}
```

---

## Configuration Properties Reference

| Property | Header Sent | Default | Required |
|----------|-------------|---------|----------|
| `portkey.api-key` | `x-portkey-api-key` | - | Yes |
| `portkey.virtual-key` | `x-portkey-virtual-key` | - | No |
| `portkey.provider` | `x-portkey-provider` | - | No |
| `portkey.provider-auth-token` | `Authorization: Bearer ...` | - | No |
| `portkey.config` | `x-portkey-config` | - | No |
| `portkey.custom-host` | `x-portkey-custom-host` | - | No |
| `portkey.base-url` | (base URL) | `https://api.portkey.ai/v1` | No |
| `portkey.timeout` | (connect + read) | `60s` | No |
| `portkey.headers.*` | (custom) | - | No |

---

## Limitations

- **No streaming** - `stream` parameter intentionally removed; SSE support is planned
- **Single endpoint** - only `/v1/chat/completions`
- **No built-in retry** - add Spring Retry or Resilience4j yourself

---

## Spring AI Integration (Optional)

Add Spring AI for `ChatClient` with `@Tool` calling, streaming, and entity mapping - all routed through Portkey.

### Additional dependency

```groovy
implementation 'org.springframework.ai:spring-ai-starter-model-openai:1.1.2'
```

The SDK auto-detects Spring AI on the classpath and configures a `ChatClient.Builder` bean that routes through Portkey.

### Tool calling with @Tool

```java
class WeatherTools {
    @Tool(description = "Get current weather for a city")
    String getWeather(@ToolParam(description = "City name") String city) {
        return weatherService.lookup(city);
    }
}

@Service
public class AssistantService {
    private final ChatClient chatClient;

    public AssistantService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String ask(String question) {
        return chatClient.prompt()
            .user(question)
            .tools(new WeatherTools())
            .call()
            .content();
    }
}
```

### Entity mapping

```java
record MovieRecommendation(String title, int year, String reason) {}

MovieRecommendation movie = chatClient.prompt()
    .user("Recommend a sci-fi movie")
    .call()
    .entity(MovieRecommendation.class);
```

### Streaming

```java
Flux<String> stream = chatClient.prompt()
    .user("Write a poem about AI")
    .stream()
    .content();
```

### Using both clients

```java
@Service
public class MyService {
    private final PortkeyClient portkey;     // Low-level, per-request headers
    private final ChatClient chatClient;      // High-level, tool calling

    public MyService(PortkeyClient portkey, ChatClient.Builder builder) {
        this.portkey = portkey;
        this.chatClient = builder.build();
    }
}
```

See [docs/spring-ai-integration.md](spring-ai-integration.md) for full details.

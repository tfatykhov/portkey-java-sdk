# Portkey Java SDK

Spring Boot client for the [Portkey AI Gateway](https://portkey.ai). Two levels of API:

- **`PortkeyClient`** - lightweight REST client with per-request header control
- **Spring AI `ChatClient`** - full-featured with `@Tool` calling, streaming, entity mapping (optional)

## Requirements

- Java 25+
- Spring Boot 3.5+
- Spring AI 1.1.2+ (optional, for `ChatClient` and tool calling)

## Installation

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

### Building from source

```bash
git clone https://github.com/tfatykhov/portkey-java-sdk.git
cd portkey-java-sdk
./gradlew build
```

## Spring Boot Quick Start

**1. Configure** in `application.yml`:

```yaml
portkey:
  api-key: pk-...
  virtual-key: my-openai-virtual-key
```

**2. Inject and use:**

```java
@Service
public class ChatService {
    private final PortkeyClient portkey;

    public ChatService(PortkeyClient portkey) {
        this.portkey = portkey;
    }

    public String chat(String prompt) {
        var response = portkey.chatCompletions().create(
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

That's it. The SDK auto-configures a `PortkeyClient` bean when `portkey.api-key` is present.

## Multimodal Messages (Vision)

Send images alongside text using content parts:

```java
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.user(List.of(
            ContentPart.text("What's in this image?"),
            ContentPart.imageUrl("https://example.com/photo.jpg", ImageContentPart.Detail.high)
        )))
        .build()
);
```

Base64-encoded images are also supported:

```java
ContentPart.imageBase64("image/png", base64String)
```

### Image Byte Array Conversion

Convert raw image bytes (JPEG, BMP, GIF, etc.) to PNG base64 content parts:

```java
import ai.portkey.utils.ImageUtils;

byte[] photoBytes = Files.readAllBytes(Path.of("photo.jpg"));

// Convert to ContentPart
var part = ImageUtils.toPngContentPart(photoBytes);

// Resize to max 800px (proportional) then convert
var resized = ImageUtils.toPngContentPart(photoBytes, 800);

// Resize with detail level
var resizedHD = ImageUtils.toPngContentPart(photoBytes, 800, ImageContentPart.Detail.high);

// Get resized PNG bytes directly
byte[] pngBytes = ImageUtils.resize(photoBytes, 800);
// 4000x3000 -> 800x600, 1920x1080 -> 800x450, 600x400 -> 600x400 (no change)
```

Includes decompression bomb protection (max 8192px per dimension).
Resize preserves aspect ratio - scales proportionally by the larger dimension.

## Tool Calling

```java
// 1. Define tools and send request
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.user("What's the weather in NYC?"))
        .tools(List.of(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "get_weather",
                "parameters", Map.of("type", "object", "properties", Map.of(
                    "city", Map.of("type", "string")
                ))
            )
        )))
        .build()
);

// 2. Process tool calls from the response
var toolCalls = response.choices().getFirst().message().getToolCalls();
for (var call : toolCalls) {
    System.out.println(call.function().name());      // "get_weather"
    System.out.println(call.function().arguments());  // {"city":"NYC"}
}

// 3. Send tool results back
var followUp = ChatCompletionRequest.builder()
    .model("gpt-4o")
    .addMessage(Message.user("What's the weather in NYC?"))
    .addMessage(Message.assistant(null, toolCalls))
    .addMessage(Message.tool("{\"temp\": \"72F\", \"condition\": \"sunny\"}", "call_abc"))
    .build();
```

## Thinking (Claude models)

```java
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("claude-sonnet-4-20250514")
        .addMessage(Message.user("Solve this step by step..."))
        .thinking(Map.of("type", "enabled", "budget_tokens", 4096))
        .build()
);
```

## All Message Roles

```java
Message.system("You are helpful.")           // System instructions
Message.developer("Coding assistant rules")  // Developer role (newer OpenAI models)
Message.user("Hello!")                        // User text message
Message.user(List.of(...))                    // User multimodal message
Message.assistant("Hi there!")               // Assistant response
Message.assistant(null, toolCalls)           // Assistant with tool calls
Message.tool(result, callId)                 // Tool response

// Named participants
Message.user("Hello!").withName("alice")
```

## Configuration Properties

```yaml
portkey:
  api-key: pk-...                        # Required
  virtual-key: my-key                    # Provider virtual key
  provider: openai                       # Direct provider auth
  provider-auth-token: sk-...            # Provider Bearer token
  config: my-config-id                   # Config-based routing
  custom-host: https://my-proxy.com      # Custom provider host
  base-url: https://api.portkey.ai/v1    # Override base URL
  timeout: 60s                           # HTTP timeout
  headers:                               # Extra headers
    x-portkey-trace-id: my-trace
```

## Per-Request Headers

Use the request customizer for trace IDs, metadata, and other per-request headers:

```java
portkey.chatCompletions().create(request, spec ->
    spec.header("x-portkey-trace-id", "my-trace-123")
        .header("x-portkey-metadata", "{\"user\":\"abc\"}")
        .header("x-portkey-cache-force-refresh", "true")
);
```

## Standalone (Without Spring Boot)

```java
var restClient = RestClient.builder()
    .baseUrl("https://api.portkey.ai/v1")
    .defaultHeader("x-portkey-api-key", "pk-...")
    .defaultHeader("x-portkey-virtual-key", "my-key")
    .build();

var client = new PortkeyClient(restClient);
```

## Self-Hosted Gateway

```yaml
portkey:
  api-key: pk-...
  virtual-key: my-key
  base-url: http://your-gateway:8080/v1
```

## Spring AI Integration (Optional)

Add Spring AI for `ChatClient` with `@Tool` calling, streaming, and entity mapping - all routed through Portkey.

### 1. Add Spring AI dependency

```groovy
implementation 'org.springframework.ai:spring-ai-openai:1.1.2'
```

The SDK auto-detects Spring AI on the classpath and configures everything.

### 2. Tool Calling

Define tools as annotated Java methods:

```java
class WeatherTools {
    @Tool(description = "Get current weather for a city")
    String getWeather(@ToolParam(description = "City name") String city) {
        // Your real implementation
        return weatherService.lookup(city);
    }

    @Tool(description = "Convert temperature between units")
    String convertTemp(@ToolParam(description = "Temperature value") double temp,
                       @ToolParam(description = "From unit: celsius or fahrenheit") String from) {
        if ("celsius".equals(from)) return "%.1f°F".formatted(temp * 9/5 + 32);
        return "%.1f°C".formatted((temp - 32) * 5/9);
    }
}
```

Use them with `ChatClient`:

```java
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

Spring AI handles the entire tool dispatch loop automatically: sends tool definitions, receives tool calls, invokes your Java methods, sends results back, returns the final answer.

### 3. Streaming

```java
Flux<String> stream = chatClient.prompt()
    .user("Write a poem about AI")
    .stream()
    .content();
```

### 4. Entity Mapping

```java
record MovieRecommendation(String title, int year, String reason) {}

MovieRecommendation movie = chatClient.prompt()
    .user("Recommend a sci-fi movie")
    .call()
    .entity(MovieRecommendation.class);
```

### 5. Two Clients, One Config

Both clients are auto-configured from the same `portkey.*` properties:

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

## Error Handling

```java
import ai.portkey.exception.PortkeyException;

try {
    var resp = portkey.chatCompletions().create(request);
} catch (PortkeyException e) {
    System.err.println("Status: " + e.getStatusCode());
    System.err.println("Message: " + e.getErrorMessage());
    System.err.println("Type: " + e.getErrorType());
    System.err.println("Code: " + e.getErrorCode());
    System.err.println("Raw body: " + e.getResponseBody());
}
```

## Retry / Resilience

The SDK does not include built-in retry logic. For production use, add your own resilience layer:

```java
// Spring Retry
@Retryable(retryFor = PortkeyException.class,
           maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
public ChatCompletionResponse chat(ChatCompletionRequest request) {
    return portkey.chatCompletions().create(request);
}

// Or Resilience4j, or your own retry loop for 429/5xx errors
```

## Supported Portkey Headers

| Property | Header | Purpose |
|----------|--------|---------|
| `api-key` | `x-portkey-api-key` | Portkey API key (required) |
| `virtual-key` | `x-portkey-virtual-key` | Provider virtual key |
| `provider` | `x-portkey-provider` | Provider name |
| `provider-auth-token` | `Authorization: Bearer` | Provider auth |
| `config` | `x-portkey-config` | Config ID for routing |
| `custom-host` | `x-portkey-custom-host` | Custom provider host |
| *per-request* | `x-portkey-trace-id` | Trace ID |
| *per-request* | `x-portkey-span-id` | Span ID |
| *per-request* | `x-portkey-parent-span-id` | Parent span ID |
| *per-request* | `x-portkey-span-name` | Span name |
| *per-request* | `x-portkey-metadata` | JSON metadata |
| *per-request* | `x-portkey-cache-namespace` | Cache namespace |
| *per-request* | `x-portkey-cache-force-refresh` | Force cache refresh |

## License

MIT

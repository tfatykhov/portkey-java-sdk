# Portkey Java SDK - Code Examples

Complete, copy-paste-ready examples for common use cases.

---

## 1. Basic Chat Completion

```java
import ai.portkey.client.PortkeyClient;
import ai.portkey.model.ChatCompletionRequest;
import ai.portkey.model.Message;

@Service
public class ChatService {
    private final PortkeyClient portkey;

    public ChatService(PortkeyClient portkey) {
        this.portkey = portkey;
    }

    public String chat(String userMessage) {
        var response = portkey.chatCompletions().create(
            ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.system("You are a helpful assistant."))
                .addMessage(Message.user(userMessage))
                .temperature(0.7)
                .maxTokens(500)
                .build()
        );
        return response.getContent();
    }
}
```

---

## 2. Multi-Turn Conversation

```java
public String converse(List<Message> history, String newMessage) {
    var builder = ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.system("You are a helpful assistant."))
        .addMessages(history)
        .addMessage(Message.user(newMessage));

    var response = portkey.chatCompletions().create(builder.build());
    return response.getContent();
}
```

---

## 3. Multimodal - Image from URL

```java
import ai.portkey.model.ContentPart;
import ai.portkey.model.ImageContentPart;

public String describeImage(String imageUrl) {
    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.user(List.of(
                ContentPart.text("What's in this image?"),
                ContentPart.imageUrl(imageUrl, ImageContentPart.Detail.high)
            )))
            .maxTokens(300)
            .build()
    );
    return response.getContent();
}
```

---

## 4. Multimodal - Base64 Image

```java
public String analyzeScreenshot(byte[] screenshotBytes) throws IOException {
    // Convert raw bytes to a content part (auto-converts to PNG base64)
    var imagePart = ImageUtils.toPngContentPart(screenshotBytes, 1024);

    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.user(List.of(
                ContentPart.text("Describe this screenshot."),
                imagePart
            )))
            .build()
    );
    return response.getContent();
}
```

---

## 5. Multimodal - PDF Document

```java
import java.nio.file.Files;
import java.nio.file.Path;

public String summarizePdf(Path pdfPath) throws IOException {
    byte[] pdfBytes = Files.readAllBytes(pdfPath);

    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.user(List.of(
                ContentPart.text("Summarize this document."),
                ContentPart.fileBytes("application/pdf", pdfBytes)
            )))
            .maxTokens(1000)
            .build()
    );
    return response.getContent();
}
```

---

## 6. Tool Calling (Function Calling)

### Complete Tool Loop

```java
import ai.portkey.model.ToolCall;
import com.fasterxml.jackson.databind.ObjectMapper;

public String chatWithTools(String userQuestion) throws Exception {
    var objectMapper = new ObjectMapper();

    // Step 1: Define tools
    var tools = List.of(Map.of(
        "type", "function",
        "function", Map.of(
            "name", "get_weather",
            "description", "Get the current weather for a city",
            "parameters", Map.of(
                "type", "object",
                "properties", Map.of(
                    "city", Map.of("type", "string", "description", "City name"),
                    "unit", Map.of("type", "string", "enum", List.of("celsius", "fahrenheit"))
                ),
                "required", List.of("city")
            )
        )
    ));

    // Step 2: Send initial request
    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.user(userQuestion))
            .tools(tools)
            .build()
    );

    var choice = response.choices().getFirst();

    // Step 3: Check if the model wants to call tools
    if ("tool_calls".equals(choice.finishReason())) {
        var toolCalls = choice.message().getToolCalls();

        // Build follow-up messages
        var messages = new ArrayList<Message>();
        messages.add(Message.user(userQuestion));
        messages.add(Message.assistant(null, toolCalls));

        // Step 4: Execute each tool call and add results
        for (var call : toolCalls) {
            String functionName = call.function().name();
            String arguments = call.function().arguments();

            // Parse arguments and execute your function
            var args = objectMapper.readTree(arguments);
            String result = executeFunction(functionName, args);

            messages.add(Message.tool(result, call.id()));
        }

        // Step 5: Send tool results back to get final answer
        var followUp = ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessages(messages)
            .tools(tools)
            .build();

        var finalResponse = portkey.chatCompletions().create(followUp);
        return finalResponse.getContent();
    }

    // No tool calls - return direct response
    return response.getContent();
}

private String executeFunction(String name, JsonNode args) {
    return switch (name) {
        case "get_weather" -> {
            String city = args.get("city").asText();
            yield "{\"temp\": \"72F\", \"condition\": \"sunny\", \"city\": \"%s\"}".formatted(city);
        }
        default -> "{\"error\": \"Unknown function: %s\"}".formatted(name);
    };
}
```

---

## 7. Claude Thinking Mode

```java
public String solveWithThinking(String problem) {
    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("claude-sonnet-4-20250514")
            .addMessage(Message.user(problem))
            .thinking(Map.of(
                "type", "enabled",
                "budget_tokens", 4096
            ))
            .build()
    );
    return response.getContent();
}
```

---

## 8. JSON Mode

```java
public String getStructuredResponse(String prompt) {
    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.system("Always respond in valid JSON."))
            .addMessage(Message.user(prompt))
            .responseFormat(Map.of("type", "json_object"))
            .build()
    );
    return response.getContent(); // Returns a JSON string
}
```

---

## 9. Per-Request Tracing and Metadata

```java
public String chatWithTracing(String prompt, String userId, String traceId) {
    var request = ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.user(prompt))
        .build();

    var response = portkey.chatCompletions().create(request, spec ->
        spec.header("x-portkey-trace-id", traceId)
            .header("x-portkey-metadata", "{\"user_id\":\"%s\"}".formatted(userId))
            .header("x-portkey-cache-namespace", "user-" + userId)
    );

    return response.getContent();
}
```

---

## 10. Error Handling with Retry

```java
import ai.portkey.exception.PortkeyException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Service
public class ResilientChatService {
    private final PortkeyClient portkey;

    public ResilientChatService(PortkeyClient portkey) {
        this.portkey = portkey;
    }

    @Retryable(
        retryFor = PortkeyException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String chat(String prompt) {
        return portkey.chatCompletions().create(
            ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user(prompt))
                .build()
        ).getContent();
    }
}
```

### Manual Retry for Rate Limits

```java
public String chatWithManualRetry(ChatCompletionRequest request) {
    int maxAttempts = 3;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return portkey.chatCompletions().create(request).getContent();
        } catch (PortkeyException e) {
            if (e.getStatusCode() == 429 && attempt < maxAttempts) {
                // Rate limited - wait and retry
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }
    throw new IllegalStateException("Should not reach here");
}
```

---

## 11. Image Resize and Conversion

```java
import ai.portkey.utils.ImageUtils;
import ai.portkey.model.ImageContentPart;

public void imageExamples() throws IOException {
    byte[] photoBytes = Files.readAllBytes(Path.of("photo.jpg"));

    // Convert any format to PNG base64 content part
    var part = ImageUtils.toPngContentPart(photoBytes);

    // Resize to max 800px, preserving aspect ratio
    var resized = ImageUtils.toPngContentPart(photoBytes, 800);

    // Resize with detail level
    var hd = ImageUtils.toPngContentPart(photoBytes, 1024, ImageContentPart.Detail.high);

    // Get raw resized PNG bytes (for saving to disk, etc.)
    byte[] pngBytes = ImageUtils.resize(photoBytes, 800);

    // Get base64 string directly
    String base64 = ImageUtils.toPngBase64(photoBytes);
    String resizedBase64 = ImageUtils.toPngBase64(photoBytes, 800);
}
```

---

## 12. Spring AI ChatClient with @Tool

Requires `spring-ai-starter-model-openai` on the classpath.

### Configuration

```yaml
spring:
  ai:
    openai:
      api-key: portkey-managed
      chat:
        options:
          model: gpt-4o

portkey:
  api-key: pk-...
  virtual-key: my-openai-key
```

### Tool Definition

```java
public class WeatherTools {
    @Tool(description = "Get current weather for a city")
    String getWeather(@ToolParam(description = "City name") String city) {
        // Your real implementation
        return "{\"temp\": \"72F\", \"condition\": \"sunny\"}";
    }

    @Tool(description = "Convert temperature between units")
    String convertTemp(
            @ToolParam(description = "Temperature value") double temp,
            @ToolParam(description = "From unit: celsius or fahrenheit") String from) {
        if ("celsius".equals(from)) return "%.1f F".formatted(temp * 9/5 + 32);
        return "%.1f C".formatted((temp - 32) * 5/9);
    }
}
```

### Service Using ChatClient

```java
@Service
public class AssistantService {
    private final ChatClient chatClient;

    public AssistantService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // Basic prompt
    public String ask(String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }

    // With tool calling (Spring AI handles the dispatch loop)
    public String askWithTools(String question) {
        return chatClient.prompt()
            .user(question)
            .tools(new WeatherTools())
            .call()
            .content();
    }

    // Streaming response
    public Flux<String> stream(String question) {
        return chatClient.prompt()
            .user(question)
            .stream()
            .content();
    }

    // Entity mapping
    public MovieRecommendation recommend(String genre) {
        return chatClient.prompt()
            .user("Recommend a " + genre + " movie")
            .call()
            .entity(MovieRecommendation.class);
    }

    record MovieRecommendation(String title, int year, String reason) {}
}
```

---

## 13. Standalone Client (No Spring Boot)

```java
import ai.portkey.client.PortkeyClient;
import ai.portkey.model.ChatCompletionRequest;
import ai.portkey.model.Message;
import org.springframework.web.client.RestClient;

public class StandaloneExample {
    public static void main(String[] args) {
        // Build RestClient manually
        var restClient = RestClient.builder()
            .baseUrl("https://api.portkey.ai/v1")
            .defaultHeader("x-portkey-api-key", System.getenv("PORTKEY_API_KEY"))
            .defaultHeader("x-portkey-virtual-key", System.getenv("PORTKEY_VIRTUAL_KEY"))
            .build();

        var portkey = new PortkeyClient(restClient);

        // Use it
        var response = portkey.chatCompletions().create(
            ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("Hello!"))
                .build()
        );

        System.out.println(response.getContent());
        System.out.println("Tokens used: " + response.usage().totalTokens());
    }
}
```

---

## 14. Using Config-Based Routing

Portkey configs enable fallbacks, load balancing, and caching at the gateway level.

```yaml
portkey:
  api-key: pk-...
  config: my-config-id    # Created in Portkey dashboard
```

```java
// The config handles routing - no virtual-key needed
var response = portkey.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.user("Hello!"))
        .build()
);
```

---

## 15. Self-Hosted Gateway

```yaml
portkey:
  api-key: pk-...
  virtual-key: my-key
  base-url: http://your-gateway:8080/v1
```

No code changes required - the client automatically uses the configured base URL.

---

## 16. Reading Response Metadata

```java
var response = portkey.chatCompletions().create(request);

// Response metadata
String id = response.id();                    // "chatcmpl-..."
String model = response.model();              // actual model used
long created = response.created();            // Unix timestamp

// Token usage
int promptTokens = response.usage().promptTokens();
int completionTokens = response.usage().completionTokens();
int totalTokens = response.usage().totalTokens();

// All choices (usually just one)
for (var choice : response.choices()) {
    int index = choice.index();
    String finishReason = choice.finishReason();  // "stop", "tool_calls", "length"
    Message message = choice.message();
    String content = message.getContentAsText();
}
```

---

## 17. Multiple Images in One Request

```java
public String compareImages(String url1, String url2) {
    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.user(List.of(
                ContentPart.text("Compare these two images. What are the differences?"),
                ContentPart.imageUrl(url1, ImageContentPart.Detail.high),
                ContentPart.imageUrl(url2, ImageContentPart.Detail.high)
            )))
            .maxTokens(500)
            .build()
    );
    return response.getContent();
}
```

---

## 18. Named Participants

```java
public String groupChat() {
    var response = portkey.chatCompletions().create(
        ChatCompletionRequest.builder()
            .model("gpt-4o")
            .addMessage(Message.system("You are mediating a conversation."))
            .addMessage(Message.user("I think we should use Java.").withName("alice"))
            .addMessage(Message.user("I prefer Python.").withName("bob"))
            .addMessage(Message.user("What about Go?").withName("charlie"))
            .build()
    );
    return response.getContent();
}
```

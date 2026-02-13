# Portkey Java SDK - API Reference

Complete API reference for all public classes in the Portkey Java SDK.

---

## PortkeyClient

**Package:** `ai.portkey.client`

The main entry point for the Portkey API. Wraps Spring's `RestClient`.

### Constructor

```java
public PortkeyClient(RestClient restClient)
```

In Spring Boot, the client is auto-configured. Inject it directly:

```java
@Service
public class MyService {
    private final PortkeyClient portkey;
    public MyService(PortkeyClient portkey) { this.portkey = portkey; }
}
```

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `chatCompletions()` | `ChatCompletions` | Access the chat completions resource |

### ChatCompletions (Inner Class)

| Method | Returns | Description |
|--------|---------|-------------|
| `create(ChatCompletionRequest)` | `ChatCompletionResponse` | Send a chat completion request |
| `create(ChatCompletionRequest, RequestCustomizer)` | `ChatCompletionResponse` | Send with per-request header customization |

### RequestCustomizer (Functional Interface)

```java
@FunctionalInterface
public interface RequestCustomizer {
    void customize(RestClient.RequestBodySpec spec);
}
```

Used as a lambda for per-request headers:

```java
portkey.chatCompletions().create(request, spec ->
    spec.header("x-portkey-trace-id", "my-trace")
);
```

---

## ChatCompletionRequest

**Package:** `ai.portkey.model`

Immutable request body for `POST /v1/chat/completions`. Built via the builder pattern.

### Builder Methods

| Method | Type | Required | Description |
|--------|------|----------|-------------|
| `model(String)` | `String` | Yes | Model identifier (e.g. `"gpt-4o"`, `"claude-sonnet-4-20250514"`) |
| `addMessage(Message)` | `Message` | Yes (1+) | Add a single message |
| `addMessage(String role, String content)` | `String, String` | - | Add a message by role and content strings |
| `addMessages(List<Message>)` | `List<Message>` | - | Bulk add messages |
| `temperature(double)` | `double` | No | Sampling temperature (0.0-2.0) |
| `topP(double)` | `double` | No | Nucleus sampling threshold |
| `n(int)` | `int` | No | Number of completions to generate |
| `stop(Object)` | `Object` | No | Stop sequence(s) - String or List<String> |
| `maxTokens(int)` | `int` | No | Maximum tokens in response |
| `presencePenalty(double)` | `double` | No | Presence penalty (-2.0 to 2.0) |
| `frequencyPenalty(double)` | `double` | No | Frequency penalty (-2.0 to 2.0) |
| `logitBias(Map<String, Integer>)` | `Map` | No | Token ID to bias value mapping |
| `user(String)` | `String` | No | End-user identifier |
| `logprobs(boolean)` | `boolean` | No | Return log probabilities |
| `topLogprobs(int)` | `int` | No | Number of top logprobs per token |
| `responseFormat(Map<String, Object>)` | `Map` | No | Response format (e.g. JSON mode) |
| `seed(int)` | `int` | No | Deterministic sampling seed |
| `tools(List<Map<String, Object>>)` | `List<Map>` | No | Tool/function definitions |
| `toolChoice(Object)` | `Object` | No | Tool selection strategy (`"auto"`, `"none"`, or specific) |
| `parallelToolCalls(boolean)` | `boolean` | No | Allow parallel tool calls |
| `thinking(Map<String, Object>)` | `Map` | No | Claude extended thinking config |
| `build()` | - | - | Build the request. Throws `IllegalStateException` if model or messages missing. |

### Validation Rules

- `model` must be non-null and non-empty
- At least one message must be added
- Messages list is copied on `build()` (immutable after build)

---

## ChatCompletionResponse

**Package:** `ai.portkey.model`

Java record representing the API response.

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Completion ID (e.g. `"chatcmpl-..."`) |
| `object` | `String` | Always `"chat.completion"` |
| `created` | `long` | Unix timestamp |
| `model` | `String` | Model that generated the response |
| `systemFingerprint` | `String` | Model system fingerprint |
| `choices` | `List<Choice>` | Completion choices |
| `usage` | `Usage` | Token usage statistics |

### Convenience Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getContent()` | `String` | Text content of the first choice's message. Returns `null` if no choices or message. |

---

## Message

**Package:** `ai.portkey.model`

Represents a chat message. Supports all roles from the OpenAI/Portkey spec.

### Factory Methods (Use These to Create Messages)

| Method | Description |
|--------|-------------|
| `Message.system(String content)` | System instructions |
| `Message.developer(String content)` | Developer role (newer OpenAI models) |
| `Message.user(String content)` | User text message |
| `Message.user(List<ContentPart> parts)` | User multimodal message (text + images + files) |
| `Message.assistant(String content)` | Assistant text response |
| `Message.assistant(String content, List<ToolCall> toolCalls)` | Assistant response with tool calls |
| `Message.tool(String content, String toolCallId)` | Tool result message |

### Instance Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getRole()` | `String` | Message role |
| `getContent()` | `Object` | Raw content (String or List<ContentPart>) |
| `getContentAsText()` | `String` | Content as String, `null` if multimodal |
| `getContentAsParts()` | `List<ContentPart>` | Content as parts, `null` if text |
| `getName()` | `String` | Participant name |
| `getToolCalls()` | `List<ToolCall>` | Tool calls (assistant messages) |
| `getToolCallId()` | `String` | Tool call ID (tool messages) |
| `withName(String name)` | `Message` | Returns a copy with the given name |

---

## ContentPart (Sealed Interface)

**Package:** `ai.portkey.model`

Sealed interface for multimodal content. Implementations: `TextContentPart`, `ImageContentPart`, `FileContentPart`.

### Factory Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `ContentPart.text(String text)` | `TextContentPart` | Text content |
| `ContentPart.imageUrl(String url)` | `ImageContentPart` | Image from URL |
| `ContentPart.imageUrl(String url, Detail detail)` | `ImageContentPart` | Image from URL with detail level |
| `ContentPart.imageBase64(String mediaType, String base64)` | `ImageContentPart` | Base64-encoded image |
| `ContentPart.imageBase64(String mediaType, String base64, Detail detail)` | `ImageContentPart` | Base64 image with detail |
| `ContentPart.file(String mimeType, String fileData)` | `FileContentPart` | File content (PDF, text, CSV) |
| `ContentPart.pdf(String base64Data)` | `FileContentPart` | PDF shorthand |
| `ContentPart.fileBytes(String mimeType, byte[] data)` | `FileContentPart` | File from raw bytes |

### ImageContentPart.Detail Enum

| Value | Description |
|-------|-------------|
| `auto` | Let the model decide |
| `low` | Faster, fewer tokens |
| `high` | More detailed analysis |

---

## Choice

**Package:** `ai.portkey.model`

Java record for a single completion choice.

| Field | Type | Description |
|-------|------|-------------|
| `index` | `int` | Choice index |
| `finishReason` | `String` | `"stop"`, `"tool_calls"`, `"length"`, etc. |
| `message` | `Message` | The assistant's response message |
| `logprobs` | `Object` | Log probabilities (if requested) |

---

## Usage

**Package:** `ai.portkey.model`

Java record for token usage statistics.

| Field | Type | Description |
|-------|------|-------------|
| `promptTokens` | `int` | Tokens used in the prompt |
| `completionTokens` | `int` | Tokens used in the completion |
| `totalTokens` | `int` | Total tokens used |

---

## ToolCall

**Package:** `ai.portkey.model`

Java record for a tool call generated by the model.

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Tool call ID (e.g. `"call_abc123"`) |
| `type` | `String` | Always `"function"` |
| `function` | `Function` | Function name and arguments |

### ToolCall.Function (Inner Record)

| Field | Type | Description |
|-------|------|-------------|
| `name` | `String` | Function name |
| `arguments` | `String` | JSON string of arguments |

---

## PortkeyException

**Package:** `ai.portkey.exception`

Thrown on API errors. Extends `RuntimeException`.

### Constructors

| Constructor | Description |
|-------------|-------------|
| `PortkeyException(int statusCode, String responseBody)` | API error with HTTP status |
| `PortkeyException(String message, Throwable cause)` | Transport/connection error |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getStatusCode()` | `int` | HTTP status code, `-1` for transport errors |
| `getResponseBody()` | `String` | Raw response body |
| `getErrorMessage()` | `String` | Parsed `error.message` field |
| `getErrorType()` | `String` | Parsed `error.type` (e.g. `"auth_error"`) |
| `getErrorCode()` | `String` | Parsed `error.code` field |

---

## ImageUtils

**Package:** `ai.portkey.utils`

Utility for converting and resizing image byte arrays. Accepts any `ImageIO`-supported format (JPEG, PNG, BMP, GIF, TIFF, WBMP). Outputs PNG.

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `MAX_DIMENSION` | `8192` | Max allowed pixel dimension (decompression bomb protection) |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `resize(byte[] imageBytes, int maxSize)` | `byte[]` | Resize proportionally, return PNG bytes |
| `toPngBase64(byte[] imageBytes)` | `String` | Convert to PNG base64 string |
| `toPngBase64(byte[] imageBytes, int maxSize)` | `String` | Resize then convert to PNG base64 |
| `toPngContentPart(byte[] imageBytes)` | `ImageContentPart` | Convert to content part |
| `toPngContentPart(byte[] imageBytes, int maxSize)` | `ImageContentPart` | Resize then convert to content part |
| `toPngContentPart(byte[] imageBytes, Detail detail)` | `ImageContentPart` | Convert with detail level |
| `toPngContentPart(byte[] imageBytes, int maxSize, Detail detail)` | `ImageContentPart` | Resize then convert with detail level |

All methods throw `IOException` on read/encode failure and `IllegalArgumentException` on null/empty input or oversized images.

---

## PortkeyProperties

**Package:** `ai.portkey.config`

Spring Boot configuration properties mapped from the `portkey.*` prefix.

| Property | YAML Key | Header Sent | Default |
|----------|----------|-------------|---------|
| `apiKey` | `portkey.api-key` | `x-portkey-api-key` | (required) |
| `virtualKey` | `portkey.virtual-key` | `x-portkey-virtual-key` | `null` |
| `provider` | `portkey.provider` | `x-portkey-provider` | `null` |
| `providerAuthToken` | `portkey.provider-auth-token` | `Authorization: Bearer` | `null` |
| `config` | `portkey.config` | `x-portkey-config` | `null` |
| `customHost` | `portkey.custom-host` | `x-portkey-custom-host` | `null` |
| `baseUrl` | `portkey.base-url` | (used as RestClient base URL) | `https://api.portkey.ai/v1` |
| `timeout` | `portkey.timeout` | (HTTP timeout) | `60s` |
| `headers` | `portkey.headers` | (sent as-is) | `null` |

---

## Portkey Per-Request Headers

These headers can be set via `RequestCustomizer` on each call:

| Header | Purpose |
|--------|---------|
| `x-portkey-trace-id` | Trace ID for observability |
| `x-portkey-span-id` | Span ID |
| `x-portkey-parent-span-id` | Parent span ID |
| `x-portkey-span-name` | Span name |
| `x-portkey-metadata` | JSON metadata string |
| `x-portkey-cache-namespace` | Cache namespace |
| `x-portkey-cache-force-refresh` | Force cache refresh (`"true"`) |

# AGENTS.md - Portkey Java SDK

## What This Is

A Spring Boot client library for the [Portkey AI Gateway](https://portkey.ai). Provides type-safe Java access to the OpenAI-compatible `/v1/chat/completions` endpoint through Portkey's unified gateway.

## Tech Stack

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 25 | Records, sealed interfaces, pattern matching (all GA, no preview flags) |
| Spring Boot | 3.5.10 | Auto-configuration, `RestClient` |
| Gradle | 9.2.0 | Build tool, `java-library` plugin |
| Spring AI | 1.1.2+ | Optional - `ChatClient`, `@Tool` calling, streaming, entity mapping |
| Jackson | (managed) | JSON serialization via Spring Boot BOM |
| JUnit 5 | (managed) | Tests with `MockRestServiceServer` |

## Project Structure

```
src/main/java/ai/portkey/
  client/
    PortkeyClient.java          # Main client (RestClient-backed, inner ChatCompletions class)
  config/
    PortkeyAutoConfiguration.java  # Spring Boot auto-config (@ConditionalOnProperty)
    PortkeyProperties.java         # Config properties (portkey.*)
    PortkeyHeaderInterceptor.java  # Spring AI RestClient interceptor (adds Portkey headers)
    PortkeySpringAiAutoConfiguration.java # Spring AI ChatClient auto-config (optional)
  exception/
    PortkeyException.java       # API error with structured parsing (message, type, code)
  model/
    ChatCompletionRequest.java  # Request builder (immutable after build)
    ChatCompletionResponse.java # Response record with getContent() convenience
    Choice.java                 # Single completion choice
    Usage.java                  # Token usage stats
    Message.java                # All roles: system, developer, user, assistant, tool
    ContentPart.java            # Sealed interface (type resolution via MessageContentDeserializer)
    TextContentPart.java        # Text content record
    ImageContentPart.java       # Image URL/base64 content record with Detail enum
    MessageContentDeserializer.java # Custom Jackson deserializer for polymorphic Message.content
    ToolCall.java               # Tool call record with Function inner record
  utils/
    ImageUtils.java             # Image conversion: resize, PNG encode, base64, ContentPart factory

src/test/java/ai/portkey/
    ModelSerializationTest.java      # Jackson serialization round-trips
    PortkeyClientIntegrationTest.java # 13 tests with MockRestServiceServer
  exception/
    PortkeyExceptionTest.java        # 10 tests for structured error parsing edge cases
  model/
    MessageContentDeserializerTest.java # 8 tests for multimodal content round-trips
    MessageTest.java                 # 13 tests for copy semantics, accessors, immutability
    ChatCompletionRequestTest.java   # 14 tests for builder edge cases
    ChatCompletionResponseTest.java  # 6 tests for getContent() edge cases
  config/
    PortkeyHeaderInterceptorTest.java    # Spring AI header interceptor tests
    PortkeySpringAiAutoConfigurationTest.java # Spring AI auto-config tests
    PortkeySpringAiIntegrationTest.java  # Spring AI integration with tool calling
  utils/
    ImageUtilsTest.java              # 24 tests for image conversion and resize
```

## Key Design Decisions

- **Library, not app**: `bootJar` disabled, `jar` enabled. This is a dependency, not a standalone service.
- **Immutable request objects**: `ChatCompletionRequest` uses `List.copyOf()` in builder. No mutation after `build()`.
- **Message quasi-immutable**: Setters are package-private (`@JsonSetter`) for Jackson deserialization only. Public API is factory methods + `withName()` (returns copy).
- **Sealed `ContentPart`**: `TextContentPart` and `ImageContentPart` records. Polymorphic deserialization handled by custom `MessageContentDeserializer` (not `@JsonTypeInfo`, which conflicts with records).
- **`@JsonIgnore` on derived accessors**: `getContentAsText()` and `getContentAsParts()` are marked `@JsonIgnore` to prevent Jackson from treating them as bean properties during deserialization.
- **No streaming**: `stream()` intentionally removed from builder. `PortkeyClient` uses blocking `RestClient` - SSE support is future work.
- **No built-in retry**: Users add their own resilience (Spring Retry, Resilience4j). Documented in README.
- **Decompression bomb protection**: `ImageUtils` checks dimensions via `ImageReader` before full decode (max 8192px).

## Conventions

- **Records over classes** for immutable data (`ChatCompletionResponse`, `Choice`, `Usage`, `ToolCall`, content parts).
- **`@JsonIgnoreProperties(ignoreUnknown = true)`** on all response records for forward compatibility.
- **`@JsonInclude(NON_NULL)`** on request/content objects to keep payloads clean.
- **Package-private test classes** (JUnit 5 standard).
- **Factory methods on `Message`** (`Message.system()`, `Message.user()`, etc.) instead of public constructors.
- **Factory methods on `ContentPart`** (`ContentPart.text()`, `ContentPart.imageUrl()`, etc.).

## Building & Testing

```bash
./gradlew build          # Compile + test
./gradlew test           # Tests only
./gradlew jar            # Build library JAR
```

CI runs on GitHub Actions with Oracle JDK 25. Gradle needs `-Dorg.gradle.java.home=$JAVA_HOME` in CI to find JDK 25.

## Common Tasks

### Add a new API endpoint
1. Add method to `PortkeyClient` (or create new inner resource class like `ChatCompletions`)
2. Create request/response records in `ai.portkey.model`
3. Add integration tests with `MockRestServiceServer`
4. Update README

### Add a new message role
1. Add static factory method to `Message.java`
2. Add test case to `ModelSerializationTest`

### Add a new content part type
1. Create new record implementing `ContentPart`
2. Add case to `MessageContentDeserializer.deserializeContentPart()` switch
3. Add factory method on `ContentPart`
4. Add test cases

### Add a new utility
1. Add to `ai.portkey.utils` package
2. Include input validation and Javadoc
3. Add comprehensive unit tests

## Known Limitations

- **No streaming**: SSE/`Flux` support not implemented
- **No GraalVM native hints**: Would need `reflect-config.json` for records
- **Single endpoint**: Only `/chat/completions` - no embeddings, images, audio, etc.

## PR Workflow

1. Create feature branch from `main`
2. Make changes, commit
3. Push branch, create PR
4. CI must pass (Oracle JDK 25, all tests green)
5. Code review before merge
6. Squash merge to `main`

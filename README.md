# Portkey Java SDK

Java client for the [Portkey AI Gateway](https://portkey.ai).

## Requirements

- Java 11+
- Maven 3.6+

## Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.portkey</groupId>
    <artifactId>portkey-java-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Quick Start

```java
import ai.portkey.client.PortkeyClient;
import ai.portkey.model.*;

PortkeyClient client = PortkeyClient.builder()
    .apiKey("pk-...")
    .virtualKey("my-openai-virtual-key")
    .build();

ChatCompletionResponse response = client.chatCompletions().create(
    ChatCompletionRequest.builder()
        .model("gpt-4o")
        .addMessage(Message.system("You are a helpful assistant."))
        .addMessage(Message.user("Hello!"))
        .temperature(0.7)
        .maxTokens(500)
        .build()
);

System.out.println(response.getContent());
System.out.println(response.getUsage());
```

## Authentication

Portkey supports multiple auth patterns. Configure them via the client builder:

```java
// Virtual key (most common)
PortkeyClient client = PortkeyClient.builder()
    .apiKey("pk-...")
    .virtualKey("my-virtual-key")
    .build();

// Direct provider auth
PortkeyClient client = PortkeyClient.builder()
    .apiKey("pk-...")
    .provider("openai")
    .providerAuth("sk-...")
    .build();

// Config-based routing
PortkeyClient client = PortkeyClient.builder()
    .apiKey("pk-...")
    .config("my-config-id")
    .build();
```

## Self-Hosted Gateway

```java
PortkeyClient client = PortkeyClient.builder()
    .apiKey("pk-...")
    .virtualKey("my-key")
    .baseUrl("http://your-gateway:8080/v1")
    .build();
```

## Portkey Headers

All Portkey-specific headers are supported:

| Builder Method | Header | Purpose |
|---------------|--------|---------|
| `apiKey()` | `x-portkey-api-key` | Portkey API key (required) |
| `virtualKey()` | `x-portkey-virtual-key` | Provider virtual key |
| `provider()` | `x-portkey-provider` | Provider name |
| `providerAuth()` | `Authorization` | Provider Bearer token |
| `config()` | `x-portkey-config` | Config ID for routing |
| `customHost()` | `x-portkey-custom-host` | Custom provider host |
| `traceId()` | `x-portkey-trace-id` | Trace ID for observability |
| `metadata()` | `x-portkey-metadata` | JSON metadata for logging |
| `cacheNamespace()` | `x-portkey-cache-namespace` | Cache namespace |
| `cacheForceRefresh()` | `x-portkey-cache-force-refresh` | Force cache refresh |

## Request Options

The `ChatCompletionRequest` builder supports all OpenAI-compatible parameters:

```java
ChatCompletionRequest.builder()
    .model("gpt-4o")
    .addMessage(Message.user("Explain quantum computing"))
    .temperature(0.5)
    .topP(0.9)
    .maxTokens(1000)
    .n(1)
    .stop("\n")
    .presencePenalty(0.1)
    .frequencyPenalty(0.1)
    .seed(42)
    .user("user-123")
    .responseFormat(Map.of("type", "json_object"))
    .build();
```

## Error Handling

```java
import ai.portkey.exception.PortkeyException;

try {
    ChatCompletionResponse resp = client.chatCompletions().create(request);
} catch (PortkeyException e) {
    System.err.println("Status: " + e.getStatusCode());
    System.err.println("Body: " + e.getResponseBody());
}
```

## License

MIT

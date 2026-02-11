# Spring AI Integration Guide

The Portkey Java SDK provides native integration with **Spring AI**, allowing you to use Portkey as a drop-in gateway for any application using `ChatClient` or `OpenAiChatModel`.

This integration automatically injects Portkey headers (`x-portkey-api-key`, `x-portkey-virtual-key`, etc.) into requests, routing them through the Portkey AI Gateway while preserving the standard Spring AI programming model.

## 📋 Prerequisites

*   **Java 25** (Platform threads are so 2024)
*   **Spring Boot 3.5.10+**
*   **Spring AI 1.0.0+**

## 📦 Dependencies

Add both the `portkey-java-sdk` and the Spring AI OpenAI starter to your build.

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'ai.portkey:portkey-java-sdk:0.1.0-SNAPSHOT'
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.0.0"
    }
}
```

### Maven

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>ai.portkey</groupId>
        <artifactId>portkey-java-sdk</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## ⚙️ Configuration

Configure Portkey in your `application.yml`. You need to set `spring.ai.openai.api-key` to a dummy value (like `portkey-managed`) because Spring AI validates its presence, but Portkey handles the actual authentication routing.

```yaml
spring:
  ai:
    openai:
      api-key: portkey-managed  # Required by Spring AI validation
      chat:
        options:
          model: gpt-4o

portkey:
  api-key: "pk-..."             # Your Portkey API Key
  virtual-key: "vk-..."         # (Optional) Virtual Key for provider routing
  config: "cfg-..."             # (Optional) Config ID for routing rules
  provider: "openai"            # (Optional) Provider name
  # provider-auth-token: "..."  # (Optional) Raw provider key if not using virtual keys
```

## 🚀 Usage

Simply inject `ChatClient.Builder` or `OpenAiChatModel`. The SDK auto-configures them to route through Portkey.

### 1. Basic Chat

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        // The builder is already pre-configured with Portkey integration
        this.chatClient = builder.build();
    }

    public String generate(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
```

### 2. Structured Output (Entity Mapping)

Map responses directly to Java records.

```java
public record MovieRecommendation(String title, int year, String reasoning) {}

public MovieRecommendation getRecommendation(String genre) {
    return chatClient.prompt()
            .user(u -> u.text("Recommend a {genre} movie").param("genre", genre))
            .call()
            .entity(MovieRecommendation.class);
}
```

### 3. Tool Calling (Function Calling)

Define your tools using `@Tool`:

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class WeatherTools {
    @Tool(description = "Get current weather for a city")
    public String getWeather(@ToolParam(description = "City name") String city) {
        return "{\"city\": \"" + city + "\", \"temp\": \"22C\", \"condition\": \"sunny\"}";
    }
}
```

Use them in your chat client:

```java
public String askWeather() {
    return chatClient.prompt()
            .user("What's the weather in Tokyo?")
            .tools(new WeatherTools()) // Register the tool
            .call()
            .content();
}
```

### 4. Multimodal (Vision)

Portkey supports Spring AI's multimodal capabilities seamlessly.

```java
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

public String analyzeImage() {
    var imageResource = new ClassPathResource("cat.jpg");
    
    return chatClient.prompt()
            .user(u -> u.text("Describe this image")
                        .media(MimeTypeUtils.IMAGE_JPEG, imageResource))
            .call()
            .content();
}
```

## 🔧 How It Works

When `portkey.api-key` is present:

1.  **Auto-Configuration**: `PortkeySpringAiAutoConfiguration` activates.
2.  **Interceptor**: A `PortkeyHeaderInterceptor` is created with your properties.
3.  **OpenAI API Override**: The default Spring AI `OpenAiApi` bean is replaced with one configured to use the Portkey Base URL (`https://api.portkey.ai/v1`) and the interceptor.
4.  **Header Injection**: Every request (chat, embedding, image) gets `x-portkey-*` headers automatically.

This ensures that all Spring AI features (streaming, retry, advisors, etc.) work natively while Portkey provides observability, caching, and routing.

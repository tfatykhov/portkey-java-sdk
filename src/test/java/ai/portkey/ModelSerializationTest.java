package ai.portkey;

import ai.portkey.model.*;
import ai.portkey.client.PortkeyClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void requestSerialization() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.system("You are helpful."))
                .addMessage(Message.user("Hello!"))
                .temperature(0.7)
                .maxTokens(100)
                .build();

        var json = mapper.writeValueAsString(req);

        assertTrue(json.contains("\"model\":\"gpt-4o\""));
        assertTrue(json.contains("\"role\":\"system\""));
        assertTrue(json.contains("\"role\":\"user\""));
        assertTrue(json.contains("\"max_tokens\":100"));
        assertTrue(json.contains("\"temperature\":0.7"));
        // null fields should be excluded
        assertFalse(json.contains("\"seed\""));
        assertFalse(json.contains("\"logprobs\""));
    }

    @Test
    void multimodalMessageSerialization() throws Exception {
        var msg = Message.user(List.of(
                ContentPart.text("What's in this image?"),
                ContentPart.imageUrl("https://example.com/photo.jpg", ImageContentPart.Detail.high)
        ));

        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"type\":\"text\""));
        assertTrue(json.contains("\"text\":\"What's in this image?\""));
        assertTrue(json.contains("\"type\":\"image_url\""));
        assertTrue(json.contains("\"url\":\"https://example.com/photo.jpg\""));
        assertTrue(json.contains("\"detail\":\"high\""));
    }

    @Test
    void developerMessageRole() throws Exception {
        var msg = Message.developer("You are a coding assistant.");
        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"role\":\"developer\""));
        assertTrue(json.contains("\"content\":\"You are a coding assistant.\""));
    }

    @Test
    void toolMessage() throws Exception {
        var msg = Message.tool("{\"result\": 42}", "call_abc123");
        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"role\":\"tool\""));
        assertTrue(json.contains("\"tool_call_id\":\"call_abc123\""));
    }

    @Test
    void assistantWithToolCalls() throws Exception {
        var toolCalls = List.of(
                new ToolCall("call_1", "function",
                        new ToolCall.Function("get_weather", "{\"city\":\"NYC\"}"))
        );
        var msg = Message.assistant(null, toolCalls);
        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"tool_calls\""));
        assertTrue(json.contains("\"id\":\"call_1\""));
        assertTrue(json.contains("\"name\":\"get_weather\""));
    }

    @Test
    void messageWithName() throws Exception {
        var msg = Message.user("Hello!").withName("alice");
        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"name\":\"alice\""));
    }

    @Test
    void responseDeserialization() throws Exception {
        var json = """
                {
                  "id": "chatcmpl-abc123",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "system_fingerprint": "fp_abc",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "stop",
                    "message": {
                      "role": "assistant",
                      "content": "Hello! How can I help?"
                    },
                    "logprobs": null
                  }],
                  "usage": {
                    "prompt_tokens": 10,
                    "completion_tokens": 8,
                    "total_tokens": 18
                  }
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);

        assertEquals("chatcmpl-abc123", resp.id());
        assertEquals("chat.completion", resp.object());
        assertEquals(1700000000L, resp.created());
        assertEquals("gpt-4o", resp.model());
        assertEquals("fp_abc", resp.systemFingerprint());

        assertNotNull(resp.choices());
        assertEquals(1, resp.choices().size());

        var choice = resp.choices().getFirst();
        assertEquals(0, choice.index());
        assertEquals("stop", choice.finishReason());
        assertEquals("assistant", choice.message().getRole());
        assertEquals("Hello! How can I help?", choice.message().getContentAsText());

        assertEquals("Hello! How can I help?", resp.getContent());

        var usage = resp.usage();
        assertEquals(10, usage.promptTokens());
        assertEquals(8, usage.completionTokens());
        assertEquals(18, usage.totalTokens());
    }

    @Test
    void responseWithToolCalls() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "tool_calls",
                    "message": {
                      "role": "assistant",
                      "content": null,
                      "tool_calls": [{
                        "id": "call_abc",
                        "type": "function",
                        "function": {
                          "name": "get_weather",
                          "arguments": "{\\"city\\":\\"NYC\\"}"
                        }
                      }]
                    },
                    "logprobs": null
                  }],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 10, "total_tokens": 15}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        var toolCalls = resp.choices().getFirst().message().getToolCalls();

        assertNotNull(toolCalls);
        assertEquals(1, toolCalls.size());
        assertEquals("call_abc", toolCalls.getFirst().id());
        assertEquals("get_weather", toolCalls.getFirst().function().name());
    }

    @Test
    void responseWithUnknownFields() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "choices": [],
                  "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
                  "some_future_field": true
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertEquals("x", resp.id());
    }

    @Test
    void builderValidation() {
        assertThrows(IllegalStateException.class, () ->
                ChatCompletionRequest.builder().model("gpt-4o").build()
        );
        assertThrows(IllegalStateException.class, () ->
                ChatCompletionRequest.builder().addMessage(Message.user("hi")).build()
        );
    }

    @Test
    void thinkingParameter() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("claude-3-5-sonnet")
                .addMessage(Message.user("Think about this"))
                .thinking(Map.of("type", "enabled", "budget_tokens", 2030))
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"thinking\""));
        assertTrue(json.contains("\"budget_tokens\""));
    }

    @Test
    void contentPartFactoryMethods() {
        var text = ContentPart.text("hello");
        assertInstanceOf(TextContentPart.class, text);
        assertEquals("text", text.type());

        var img = ContentPart.imageUrl("https://example.com/img.png");
        assertInstanceOf(ImageContentPart.class, img);
        assertEquals("image_url", img.type());

        var imgDetail = ContentPart.imageUrl("https://example.com/img.png", ImageContentPart.Detail.low);
        assertInstanceOf(ImageContentPart.class, imgDetail);
    }

    @Test
    void sealedInterfacePermits() {
        // Verify sealed interface exhaustive pattern matching
        ContentPart text = ContentPart.text("hi");
        var result = switch (text) {
            case TextContentPart t -> "text: " + t.text();
            case ImageContentPart i -> "image: " + i.imageUrl().url();
            case FileContentPart f -> "file: " + f.file().mimeType();
        };
        assertEquals("text: hi", result);

        ContentPart img = ContentPart.imageUrl("https://example.com/img.png");
        var result2 = switch (img) {
            case TextContentPart t -> "text: " + t.text();
            case ImageContentPart i -> "image: " + i.imageUrl().url();
            case FileContentPart f -> "file: " + f.file().mimeType();
        };
        assertEquals("image: https://example.com/img.png", result2);
    }

    @Test
    void base64ImageSerialization() throws Exception {
        var base64Data = "iVBORw0KGgoAAAANSUhEUg==";
        var msg = Message.user(List.of(
                ContentPart.text("What's in this image?"),
                ContentPart.imageBase64("image/png", base64Data)
        ));

        var json = mapper.writeValueAsString(msg);

        assertTrue(json.contains("\"url\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUg==\""));
    }

    @Test
    void base64ImageWithDetail() throws Exception {
        var part = ContentPart.imageBase64("image/jpeg", "abc123", ImageContentPart.Detail.low);
        var json = mapper.writeValueAsString(part);

        assertTrue(json.contains("\"url\":\"data:image/jpeg;base64,abc123\""));
        assertTrue(json.contains("\"detail\":\"low\""));
    }
}

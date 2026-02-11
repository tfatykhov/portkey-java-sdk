package ai.portkey.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MessageContentDeserializer} - ensures multimodal
 * content round-trips through Jackson as proper {@link ContentPart} instances.
 */
class MessageContentDeserializerTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void roundTripMultimodalMessage() throws Exception {
        var original = Message.user(List.of(
                ContentPart.text("What's in this image?"),
                ContentPart.imageUrl("https://example.com/photo.jpg")
        ));

        var json = mapper.writeValueAsString(original);
        var deserialized = mapper.readValue(json, Message.class);

        assertEquals("user", deserialized.getRole());
        assertNull(deserialized.getContentAsText());

        var parts = deserialized.getContentAsParts();
        assertNotNull(parts, "content should deserialize as List<ContentPart>");
        assertEquals(2, parts.size());

        assertInstanceOf(TextContentPart.class, parts.get(0));
        assertEquals("What's in this image?", ((TextContentPart) parts.get(0)).text());

        assertInstanceOf(ImageContentPart.class, parts.get(1));
        assertEquals("https://example.com/photo.jpg",
                ((ImageContentPart) parts.get(1)).imageUrl().url());
    }

    @Test
    void roundTripWithImageDetail() throws Exception {
        var original = Message.user(List.of(
                ContentPart.text("Describe this"),
                ContentPart.imageUrl("https://example.com/hi-res.png", ImageContentPart.Detail.high)
        ));

        var json = mapper.writeValueAsString(original);
        var deserialized = mapper.readValue(json, Message.class);

        var parts = deserialized.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());

        var imgPart = (ImageContentPart) parts.get(1);
        assertEquals(ImageContentPart.Detail.high, imgPart.imageUrl().detail());
    }

    @Test
    void roundTripBase64Image() throws Exception {
        var original = Message.user(List.of(
                ContentPart.text("What is this?"),
                ContentPart.imageBase64("image/png", "iVBORw0KGgoAAAANSUhEUg==")
        ));

        var json = mapper.writeValueAsString(original);
        var deserialized = mapper.readValue(json, Message.class);

        var parts = deserialized.getContentAsParts();
        assertNotNull(parts);

        var imgPart = (ImageContentPart) parts.get(1);
        assertTrue(imgPart.imageUrl().url().startsWith("data:image/png;base64,"));
    }

    @Test
    void deserializeStringContent() throws Exception {
        var json = """
                {
                  "role": "assistant",
                  "content": "Hello! How can I help?"
                }
                """;

        var msg = mapper.readValue(json, Message.class);

        assertEquals("assistant", msg.getRole());
        assertEquals("Hello! How can I help?", msg.getContentAsText());
        assertNull(msg.getContentAsParts());
    }

    @Test
    void deserializeNullContent() throws Exception {
        var json = """
                {
                  "role": "assistant",
                  "content": null,
                  "tool_calls": [{
                    "id": "call_1",
                    "type": "function",
                    "function": {"name": "get_weather", "arguments": "{}"}
                  }]
                }
                """;

        var msg = mapper.readValue(json, Message.class);

        assertEquals("assistant", msg.getRole());
        assertNull(msg.getContent());
        assertNull(msg.getContentAsText());
        assertNull(msg.getContentAsParts());
        assertNotNull(msg.getToolCalls());
    }

    @Test
    void deserializeMultimodalFromApiResponse() throws Exception {
        // Simulates what the API might return in a multimodal echo/response
        var json = """
                {
                  "role": "user",
                  "content": [
                    {"type": "text", "text": "Describe this image"},
                    {"type": "image_url", "image_url": {"url": "https://cdn.example.com/img.jpg", "detail": "low"}}
                  ]
                }
                """;

        var msg = mapper.readValue(json, Message.class);

        assertEquals("user", msg.getRole());
        assertNull(msg.getContentAsText());

        var parts = msg.getContentAsParts();
        assertNotNull(parts, "array content should produce ContentPart list");
        assertEquals(2, parts.size());

        var textPart = (TextContentPart) parts.get(0);
        assertEquals("text", textPart.type());
        assertEquals("Describe this image", textPart.text());

        var imgPart = (ImageContentPart) parts.get(1);
        assertEquals("image_url", imgPart.type());
        assertEquals("https://cdn.example.com/img.jpg", imgPart.imageUrl().url());
        assertEquals(ImageContentPart.Detail.low, imgPart.imageUrl().detail());
    }

    @Test
    void deserializeFullResponseWithMultimodalHistory() throws Exception {
        // Full ChatCompletionResponse where the choices message references
        // a prior multimodal conversation
        var json = """
                {
                  "id": "chatcmpl-xyz",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "stop",
                    "message": {
                      "role": "assistant",
                      "content": "The image shows a sunset over the ocean."
                    }
                  }],
                  "usage": {"prompt_tokens": 200, "completion_tokens": 12, "total_tokens": 212}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        var content = resp.choices().getFirst().message().getContentAsText();
        assertEquals("The image shows a sunset over the ocean.", content);
    }

    @Test
    void patternMatchingWorksAfterDeserialization() throws Exception {
        var json = """
                {
                  "role": "user",
                  "content": [
                    {"type": "text", "text": "hi"},
                    {"type": "image_url", "image_url": {"url": "https://x.com/img.png"}}
                  ]
                }
                """;

        var msg = mapper.readValue(json, Message.class);
        var parts = msg.getContentAsParts();
        assertNotNull(parts);

        // Verify sealed interface pattern matching works on deserialized instances
        for (ContentPart part : parts) {
            var desc = switch (part) {
                case TextContentPart t -> "text:" + t.text();
                case ImageContentPart i -> "img:" + i.imageUrl().url();
            };
            assertNotNull(desc);
        }
    }
}

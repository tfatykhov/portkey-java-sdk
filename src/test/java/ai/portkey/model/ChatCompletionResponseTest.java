package ai.portkey.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChatCompletionResponse} edge cases and convenience methods.
 */
class ChatCompletionResponseTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void getContentWithEmptyChoices() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "choices": [],
                  "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertNull(resp.getContent());
        assertTrue(resp.choices().isEmpty());
    }

    @Test
    void getContentWithNullMessage() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "choices": [{"index": 0, "finish_reason": "stop", "message": null}],
                  "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertNull(resp.getContent());
    }

    @Test
    void getContentReturnsFirstChoice() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "choices": [
                    {"index": 0, "finish_reason": "stop", "message": {"role": "assistant", "content": "first"}},
                    {"index": 1, "finish_reason": "stop", "message": {"role": "assistant", "content": "second"}}
                  ],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 2, "total_tokens": 7}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertEquals("first", resp.getContent());
    }

    @Test
    void getContentReturnsNullForToolCallResponse() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "tool_calls",
                    "message": {
                      "role": "assistant",
                      "content": null,
                      "tool_calls": [{"id": "c1", "type": "function", "function": {"name": "fn", "arguments": "{}"}}]
                    }
                  }],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 5, "total_tokens": 10}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertNull(resp.getContent());
        assertNotNull(resp.choices().getFirst().message().getToolCalls());
    }

    @Test
    void handlesUnknownFinishReason() throws Exception {
        var json = """
                {
                  "id": "x",
                  "object": "chat.completion",
                  "created": 1,
                  "model": "m",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "content_filter",
                    "message": {"role": "assistant", "content": ""},
                    "logprobs": null
                  }],
                  "usage": {"prompt_tokens": 5, "completion_tokens": 0, "total_tokens": 5}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertEquals("content_filter", resp.choices().getFirst().finishReason());
    }

    @Test
    void preservesAllFields() throws Exception {
        var json = """
                {
                  "id": "chatcmpl-full",
                  "object": "chat.completion",
                  "created": 1700000000,
                  "model": "gpt-4o-2024-05-13",
                  "system_fingerprint": "fp_abc123",
                  "choices": [{
                    "index": 0,
                    "finish_reason": "stop",
                    "message": {"role": "assistant", "content": "test"},
                    "logprobs": {"content": []}
                  }],
                  "usage": {"prompt_tokens": 10, "completion_tokens": 1, "total_tokens": 11}
                }
                """;

        var resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertEquals("chatcmpl-full", resp.id());
        assertEquals("chat.completion", resp.object());
        assertEquals(1700000000L, resp.created());
        assertEquals("gpt-4o-2024-05-13", resp.model());
        assertEquals("fp_abc123", resp.systemFingerprint());
        assertNotNull(resp.choices().getFirst().logprobs());
    }
}

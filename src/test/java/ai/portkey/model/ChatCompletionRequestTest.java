package ai.portkey.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChatCompletionRequest} builder edge cases and serialization
 * of less-commonly-used parameters.
 */
class ChatCompletionRequestTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void addMessagesList() throws Exception {
        var msgs = List.of(Message.user("one"), Message.user("two"), Message.user("three"));
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessages(msgs)
                .build();

        var json = mapper.writeValueAsString(req);
        assertEquals(3, req.getMessages().size());
        assertTrue(json.contains("\"one\""));
        assertTrue(json.contains("\"three\""));
    }

    @Test
    void stopAsString() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .stop("END")
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"stop\":\"END\""));
    }

    @Test
    void stopAsList() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .stop(List.of("STOP", "END", "DONE"))
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"STOP\""));
        assertTrue(json.contains("\"END\""));
        assertTrue(json.contains("\"DONE\""));
    }

    @Test
    void logitBias() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .logitBias(Map.of("50256", -100, "1234", 50))
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"logit_bias\""));
        assertTrue(json.contains("-100"));
    }

    @Test
    void toolChoiceAsString() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .tools(List.of(Map.of("type", "function", "function", Map.of("name", "fn"))))
                .toolChoice("auto")
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"tool_choice\":\"auto\""));
    }

    @Test
    void toolChoiceAsObject() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .tools(List.of(Map.of("type", "function", "function", Map.of("name", "get_weather"))))
                .toolChoice(Map.of("type", "function", "function", Map.of("name", "get_weather")))
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"tool_choice\""));
        assertTrue(json.contains("\"get_weather\""));
    }

    @Test
    void parallelToolCalls() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .parallelToolCalls(false)
                .build();

        var json = mapper.writeValueAsString(req);
        assertTrue(json.contains("\"parallel_tool_calls\":false"));
    }

    @Test
    void builderRejectsNullModel() {
        assertThrows(IllegalStateException.class, () ->
                ChatCompletionRequest.builder()
                        .addMessage(Message.user("test"))
                        .build()
        );
    }

    @Test
    void builderRejectsEmptyModel() {
        assertThrows(IllegalStateException.class, () ->
                ChatCompletionRequest.builder()
                        .model("")
                        .addMessage(Message.user("test"))
                        .build()
        );
    }

    @Test
    void builderRejectsNoMessages() {
        assertThrows(IllegalStateException.class, () ->
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .build()
        );
    }

    @Test
    void messagesListIsImmutable() {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> req.getMessages().add(Message.user("extra")));
    }

    @Test
    void toolsListIsImmutable() {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .tools(List.of(Map.of("type", "function")))
                .build();

        assertThrows(UnsupportedOperationException.class,
                () -> req.getTools().add(Map.of("type", "extra")));
    }

    @Test
    void nullFieldsExcludedFromJson() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .build();

        var json = mapper.writeValueAsString(req);
        assertFalse(json.contains("\"temperature\""));
        assertFalse(json.contains("\"top_p\""));
        assertFalse(json.contains("\"seed\""));
        assertFalse(json.contains("\"tools\""));
        assertFalse(json.contains("\"thinking\""));
        assertFalse(json.contains("\"logprobs\""));
    }

    @Test
    void addMessageByRoleAndContent() throws Exception {
        var req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage("user", "hello")
                .build();

        assertEquals(1, req.getMessages().size());
        assertEquals("user", req.getMessages().getFirst().getRole());
        assertEquals("hello", req.getMessages().getFirst().getContentAsText());
    }
}

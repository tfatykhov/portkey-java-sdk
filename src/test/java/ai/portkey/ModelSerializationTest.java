package ai.portkey;

import ai.portkey.model.ChatCompletionRequest;
import ai.portkey.model.ChatCompletionResponse;
import ai.portkey.model.Choice;
import ai.portkey.model.Message;
import ai.portkey.model.Usage;
import ai.portkey.client.PortkeyClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void requestSerialization() throws Exception {
        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.system("You are helpful."))
                .addMessage(Message.user("Hello!"))
                .temperature(0.7)
                .maxTokens(100)
                .build();

        String json = mapper.writeValueAsString(req);

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
    void responseDeserialization() throws Exception {
        String json = "{\n" +
                "  \"id\": \"chatcmpl-abc123\",\n" +
                "  \"object\": \"chat.completion\",\n" +
                "  \"created\": 1700000000,\n" +
                "  \"model\": \"gpt-4o\",\n" +
                "  \"system_fingerprint\": \"fp_abc\",\n" +
                "  \"choices\": [{\n" +
                "    \"index\": 0,\n" +
                "    \"finish_reason\": \"stop\",\n" +
                "    \"message\": {\n" +
                "      \"role\": \"assistant\",\n" +
                "      \"content\": \"Hello! How can I help?\"\n" +
                "    },\n" +
                "    \"logprobs\": null\n" +
                "  }],\n" +
                "  \"usage\": {\n" +
                "    \"prompt_tokens\": 10,\n" +
                "    \"completion_tokens\": 8,\n" +
                "    \"total_tokens\": 18\n" +
                "  }\n" +
                "}";

        ChatCompletionResponse resp = mapper.readValue(json, ChatCompletionResponse.class);

        assertEquals("chatcmpl-abc123", resp.getId());
        assertEquals("chat.completion", resp.getObject());
        assertEquals(1700000000L, resp.getCreated());
        assertEquals("gpt-4o", resp.getModel());
        assertEquals("fp_abc", resp.getSystemFingerprint());

        assertNotNull(resp.getChoices());
        assertEquals(1, resp.getChoices().size());

        Choice choice = resp.getChoices().get(0);
        assertEquals(0, choice.getIndex());
        assertEquals("stop", choice.getFinishReason());
        assertEquals("assistant", choice.getMessage().getRole());
        assertEquals("Hello! How can I help?", choice.getMessage().getContent());

        assertEquals("Hello! How can I help?", resp.getContent());

        Usage usage = resp.getUsage();
        assertEquals(10, usage.getPromptTokens());
        assertEquals(8, usage.getCompletionTokens());
        assertEquals(18, usage.getTotalTokens());
    }

    @Test
    void responseWithUnknownFields() throws Exception {
        String json = "{\n" +
                "  \"id\": \"x\",\n" +
                "  \"object\": \"chat.completion\",\n" +
                "  \"created\": 1,\n" +
                "  \"model\": \"m\",\n" +
                "  \"choices\": [],\n" +
                "  \"some_future_field\": true\n" +
                "}";

        ChatCompletionResponse resp = mapper.readValue(json, ChatCompletionResponse.class);
        assertEquals("x", resp.getId());
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
    void clientBuilderRequiresApiKey() {
        assertThrows(IllegalStateException.class, () ->
                PortkeyClient.builder().build()
        );
    }

    @Test
    void clientBuilderWithApiKey() {
        PortkeyClient client = PortkeyClient.builder()
                .apiKey("pk-test")
                .build();

        assertNotNull(client);
        assertNotNull(client.chatCompletions());
    }

    @Test
    void messageFactoryMethods() {
        Message sys = Message.system("Be helpful");
        assertEquals("system", sys.getRole());
        assertEquals("Be helpful", sys.getContent());

        Message usr = Message.user("Hello");
        assertEquals("user", usr.getRole());

        Message asst = Message.assistant("Hi there");
        assertEquals("assistant", asst.getRole());
    }
}

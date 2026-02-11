package ai.portkey;

import ai.portkey.client.PortkeyClient;
import ai.portkey.exception.PortkeyException;
import ai.portkey.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PortkeyClientIntegrationTest {

    private MockRestServiceServer mockServer;
    private PortkeyClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder()
                .baseUrl("https://api.portkey.ai/v1")
                .defaultHeader("x-portkey-api-key", "pk-test-key")
                .defaultHeader("x-portkey-virtual-key", "vk-test");

        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new PortkeyClient(builder.build());
    }

    // -- successful completions --

    @Test
    void chatCompletion_textResponse() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-portkey-api-key", "pk-test-key"))
                .andExpect(header("x-portkey-virtual-key", "vk-test"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("Hello!"))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.max_tokens").value(100))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-test1",
                          "object": "chat.completion",
                          "created": 1700000000,
                          "model": "gpt-4o",
                          "system_fingerprint": "fp_test",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "Hi there!"},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 15, "completion_tokens": 4, "total_tokens": 19}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.system("You are helpful."))
                        .addMessage(Message.user("Hello!"))
                        .temperature(0.7)
                        .maxTokens(100)
                        .build()
        );

        assertEquals("chatcmpl-test1", response.id());
        assertEquals("gpt-4o", response.model());
        assertEquals("Hi there!", response.getContent());
        assertEquals("stop", response.choices().getFirst().finishReason());
        assertEquals(15, response.usage().promptTokens());
        assertEquals(4, response.usage().completionTokens());
        assertEquals(19, response.usage().totalTokens());
        assertEquals("fp_test", response.systemFingerprint());

        mockServer.verify();
    }

    // -- multimodal --

    @Test
    void chatCompletion_multimodalImageUrl() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[0].content[0].type").value("text"))
                .andExpect(jsonPath("$.messages[0].content[0].text").value("Describe this image"))
                .andExpect(jsonPath("$.messages[0].content[1].type").value("image_url"))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url").value("https://example.com/cat.jpg"))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.detail").value("high"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-vision",
                          "object": "chat.completion",
                          "created": 1700000001,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "A cute orange cat."},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 100, "completion_tokens": 10, "total_tokens": 110}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user(List.of(
                                ContentPart.text("Describe this image"),
                                ContentPart.imageUrl("https://example.com/cat.jpg", ImageContentPart.Detail.high)
                        )))
                        .build()
        );

        assertEquals("A cute orange cat.", response.getContent());
        mockServer.verify();
    }

    @Test
    void chatCompletion_multimodalBase64() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url")
                        .value("data:image/png;base64,iVBORtest=="))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-b64",
                          "object": "chat.completion",
                          "created": 1700000002,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "Got the image."},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 50, "completion_tokens": 5, "total_tokens": 55}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user(List.of(
                                ContentPart.text("What is this?"),
                                ContentPart.imageBase64("image/png", "iVBORtest==")
                        )))
                        .build()
        );

        assertEquals("Got the image.", response.getContent());
        mockServer.verify();
    }

    // -- tool calling --

    @Test
    void chatCompletion_toolCalling() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.tools[0].type").value("function"))
                .andExpect(jsonPath("$.tools[0].function.name").value("get_weather"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-tools",
                          "object": "chat.completion",
                          "created": 1700000003,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "tool_calls",
                            "message": {
                              "role": "assistant",
                              "content": null,
                              "tool_calls": [{
                                "id": "call_abc123",
                                "type": "function",
                                "function": {
                                  "name": "get_weather",
                                  "arguments": "{\\"city\\":\\"NYC\\"}"
                                }
                              }]
                            },
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 30, "completion_tokens": 15, "total_tokens": 45}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user("What's the weather in NYC?"))
                        .tools(List.of(Map.of(
                                "type", "function",
                                "function", Map.of(
                                        "name", "get_weather",
                                        "parameters", Map.of("type", "object",
                                                "properties", Map.of("city", Map.of("type", "string")))
                                )
                        )))
                        .build()
        );

        assertEquals("tool_calls", response.choices().getFirst().finishReason());
        assertNull(response.getContent());

        var toolCalls = response.choices().getFirst().message().getToolCalls();
        assertEquals(1, toolCalls.size());
        assertEquals("call_abc123", toolCalls.getFirst().id());
        assertEquals("function", toolCalls.getFirst().type());
        assertEquals("get_weather", toolCalls.getFirst().function().name());
        assertEquals("{\"city\":\"NYC\"}", toolCalls.getFirst().function().arguments());

        mockServer.verify();
    }

    @Test
    void chatCompletion_toolResponse() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.messages[1].tool_calls[0].id").value("call_abc"))
                .andExpect(jsonPath("$.messages[2].role").value("tool"))
                .andExpect(jsonPath("$.messages[2].tool_call_id").value("call_abc"))
                .andExpect(jsonPath("$.messages[2].content").value("{\"temp\":\"72F\"}"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-toolresp",
                          "object": "chat.completion",
                          "created": 1700000004,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "It's 72F in NYC."},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 40, "completion_tokens": 8, "total_tokens": 48}
                        }
                        """, MediaType.APPLICATION_JSON));

        var toolCalls = List.of(
                new ToolCall("call_abc", "function", new ToolCall.Function("get_weather", "{\"city\":\"NYC\"}"))
        );

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user("What's the weather in NYC?"))
                        .addMessage(Message.assistant(null, toolCalls))
                        .addMessage(Message.tool("{\"temp\":\"72F\"}", "call_abc"))
                        .build()
        );

        assertEquals("It's 72F in NYC.", response.getContent());
        mockServer.verify();
    }

    // -- message roles --

    @Test
    void chatCompletion_developerRole() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[0].role").value("developer"))
                .andExpect(jsonPath("$.messages[0].content").value("Coding assistant rules"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-dev",
                          "object": "chat.completion",
                          "created": 1700000005,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "Understood."},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 10, "completion_tokens": 2, "total_tokens": 12}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.developer("Coding assistant rules"))
                        .addMessage(Message.user("Hello"))
                        .build()
        );

        assertEquals("Understood.", response.getContent());
        mockServer.verify();
    }

    @Test
    void chatCompletion_namedParticipants() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[0].name").value("alice"))
                .andExpect(jsonPath("$.messages[1].name").value("bob"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-named",
                          "object": "chat.completion",
                          "created": 1700000006,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "Hello Alice and Bob!"},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 12, "completion_tokens": 5, "total_tokens": 17}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user("Hi from Alice").withName("alice"))
                        .addMessage(Message.user("Hi from Bob").withName("bob"))
                        .build()
        );

        assertEquals("Hello Alice and Bob!", response.getContent());
        mockServer.verify();
    }

    // -- thinking --

    @Test
    void chatCompletion_thinking() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.thinking.type").value("enabled"))
                .andExpect(jsonPath("$.thinking.budget_tokens").value(4096))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-think",
                          "object": "chat.completion",
                          "created": 1700000007,
                          "model": "claude-sonnet-4-20250514",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "After thinking... the answer is 42."},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 20, "completion_tokens": 50, "total_tokens": 70}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("claude-sonnet-4-20250514")
                        .addMessage(Message.user("Think about the meaning of life"))
                        .thinking(Map.of("type", "enabled", "budget_tokens", 4096))
                        .build()
        );

        assertEquals("After thinking... the answer is 42.", response.getContent());
        mockServer.verify();
    }

    // -- per-request headers --

    @Test
    void chatCompletion_perRequestHeaders() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(header("x-portkey-trace-id", "trace-xyz"))
                .andExpect(header("x-portkey-cache-force-refresh", "true"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-headers",
                          "object": "chat.completion",
                          "created": 1700000008,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "OK"},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 5, "completion_tokens": 1, "total_tokens": 6}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user("test"))
                        .build(),
                spec -> spec
                        .header("x-portkey-trace-id", "trace-xyz")
                        .header("x-portkey-cache-force-refresh", "true")
        );

        assertEquals("OK", response.getContent());
        mockServer.verify();
    }

    // -- error handling --

    @Test
    void chatCompletion_unauthorized() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andRespond(withUnauthorizedRequest()
                        .body("{\"error\":{\"message\":\"Invalid API key\",\"type\":\"auth_error\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        var request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .build();

        var ex = assertThrows(PortkeyException.class,
                () -> client.chatCompletions().create(request));

        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getResponseBody().contains("Invalid API key"));
        mockServer.verify();
    }

    @Test
    void chatCompletion_rateLimited() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andRespond(withTooManyRequests()
                        .body("{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"rate_limit_error\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        var request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .build();

        var ex = assertThrows(PortkeyException.class,
                () -> client.chatCompletions().create(request));

        assertEquals(429, ex.getStatusCode());
        assertTrue(ex.getResponseBody().contains("Rate limit"));
        mockServer.verify();
    }

    @Test
    void chatCompletion_serverError() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andRespond(withServerError()
                        .body("{\"error\":{\"message\":\"Internal server error\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        var request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .addMessage(Message.user("test"))
                .build();

        var ex = assertThrows(PortkeyException.class,
                () -> client.chatCompletions().create(request));

        assertEquals(500, ex.getStatusCode());
        mockServer.verify();
    }

    // -- request params --

    @Test
    void chatCompletion_allRequestParams() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.model").value("gpt-4o"))
                .andExpect(jsonPath("$.temperature").value(0.5))
                .andExpect(jsonPath("$.top_p").value(0.9))
                .andExpect(jsonPath("$.n").value(2))
                .andExpect(jsonPath("$.max_tokens").value(500))
                .andExpect(jsonPath("$.presence_penalty").value(0.1))
                .andExpect(jsonPath("$.frequency_penalty").value(0.2))
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.user").value("user-123"))
                .andExpect(jsonPath("$.logprobs").value(true))
                .andExpect(jsonPath("$.top_logprobs").value(3))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-params",
                          "object": "chat.completion",
                          "created": 1700000009,
                          "model": "gpt-4o",
                          "choices": [{
                            "index": 0,
                            "finish_reason": "stop",
                            "message": {"role": "assistant", "content": "{}"},
                            "logprobs": null
                          }],
                          "usage": {"prompt_tokens": 10, "completion_tokens": 1, "total_tokens": 11}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user("Return JSON"))
                        .temperature(0.5)
                        .topP(0.9)
                        .n(2)
                        .maxTokens(500)
                        .presencePenalty(0.1)
                        .frequencyPenalty(0.2)
                        .seed(42)
                        .user("user-123")
                        .logprobs(true)
                        .topLogprobs(3)
                        .responseFormat(Map.of("type", "json_object"))
                        .build()
        );

        assertNotNull(response);
        mockServer.verify();
    }

    // -- multiple choices --

    @Test
    void chatCompletion_multipleChoices() {
        mockServer.expect(requestTo("https://api.portkey.ai/v1/chat/completions"))
                .andExpect(jsonPath("$.n").value(3))
                .andRespond(withSuccess("""
                        {
                          "id": "chatcmpl-multi",
                          "object": "chat.completion",
                          "created": 1700000010,
                          "model": "gpt-4o",
                          "choices": [
                            {"index": 0, "finish_reason": "stop", "message": {"role": "assistant", "content": "Option A"}, "logprobs": null},
                            {"index": 1, "finish_reason": "stop", "message": {"role": "assistant", "content": "Option B"}, "logprobs": null},
                            {"index": 2, "finish_reason": "stop", "message": {"role": "assistant", "content": "Option C"}, "logprobs": null}
                          ],
                          "usage": {"prompt_tokens": 10, "completion_tokens": 15, "total_tokens": 25}
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.chatCompletions().create(
                ChatCompletionRequest.builder()
                        .model("gpt-4o")
                        .addMessage(Message.user("Give options"))
                        .n(3)
                        .build()
        );

        assertEquals(3, response.choices().size());
        assertEquals("Option A", response.getContent()); // first choice
        assertEquals("Option B", response.choices().get(1).message().getContentAsText());
        assertEquals("Option C", response.choices().get(2).message().getContentAsText());
        mockServer.verify();
    }
}

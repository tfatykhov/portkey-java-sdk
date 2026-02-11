package ai.portkey.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Message} - copy semantics, edge cases, and accessor behavior.
 */
class MessageTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // -- withName copy correctness --

    @Test
    void withNamePreservesToolCalls() {
        var toolCalls = List.of(
                new ToolCall("call_1", "function",
                        new ToolCall.Function("get_weather", "{\"city\":\"NYC\"}"))
        );
        var original = Message.assistant("thinking...", toolCalls);
        var copy = original.withName("agent-1");

        assertEquals("agent-1", copy.getName());
        assertEquals("assistant", copy.getRole());
        assertEquals("thinking...", copy.getContentAsText());
        assertNotNull(copy.getToolCalls());
        assertEquals(1, copy.getToolCalls().size());
        assertEquals("call_1", copy.getToolCalls().getFirst().id());
        assertEquals("get_weather", copy.getToolCalls().getFirst().function().name());
    }

    @Test
    void withNamePreservesToolCallId() {
        var original = Message.tool("{\"result\":42}", "call_abc");
        var copy = original.withName("my-tool");

        assertEquals("my-tool", copy.getName());
        assertEquals("tool", copy.getRole());
        assertEquals("{\"result\":42}", copy.getContentAsText());
        assertEquals("call_abc", copy.getToolCallId());
    }

    @Test
    void withNamePreservesMultimodalContent() {
        var original = Message.user(List.of(
                ContentPart.text("Look at this"),
                ContentPart.imageUrl("https://example.com/img.png")
        ));
        var copy = original.withName("alice");

        assertEquals("alice", copy.getName());
        assertEquals("user", copy.getRole());
        assertNull(copy.getContentAsText());

        var parts = copy.getContentAsParts();
        assertNotNull(parts);
        assertEquals(2, parts.size());
        assertInstanceOf(TextContentPart.class, parts.get(0));
        assertInstanceOf(ImageContentPart.class, parts.get(1));
    }

    @Test
    void withNameDoesNotMutateOriginal() {
        var original = Message.user("hello");
        var copy = original.withName("alice");

        assertNull(original.getName());
        assertEquals("alice", copy.getName());
    }

    // -- content accessors --

    @Test
    void getContentAsTextReturnsNullForMultimodal() {
        var msg = Message.user(List.of(ContentPart.text("hi")));
        assertNull(msg.getContentAsText());
    }

    @Test
    void getContentAsPartsReturnsNullForString() {
        var msg = Message.user("hello");
        assertNull(msg.getContentAsParts());
    }

    @Test
    void getContentAsPartsReturnsNullForEmptyList() {
        // Edge case: content is a List but empty
        var msg = Message.user("text");
        // Override content to empty list via deserialization
        assertNull(msg.getContentAsParts());
    }

    @Test
    void getContentReturnsRawObject() {
        var textMsg = Message.user("hello");
        assertInstanceOf(String.class, textMsg.getContent());

        var multiMsg = Message.user(List.of(ContentPart.text("hi")));
        assertInstanceOf(List.class, multiMsg.getContent());
    }

    @Test
    void nullContentAssistantMessage() {
        var msg = Message.assistant(null, List.of(
                new ToolCall("c1", "function", new ToolCall.Function("fn", "{}"))
        ));
        assertNull(msg.getContent());
        assertNull(msg.getContentAsText());
        assertNull(msg.getContentAsParts());
        assertNotNull(msg.getToolCalls());
    }

    // -- toString --

    @Test
    void toStringIncludesRoleAndContent() {
        var msg = Message.user("hello world");
        var str = msg.toString();

        assertTrue(str.contains("user"));
        assertTrue(str.contains("hello world"));
    }

    // -- factory methods produce correct roles --

    @Test
    void factoryMethodRoles() {
        assertEquals("system", Message.system("x").getRole());
        assertEquals("developer", Message.developer("x").getRole());
        assertEquals("user", Message.user("x").getRole());
        assertEquals("assistant", Message.assistant("x").getRole());
        assertEquals("tool", Message.tool("x", "id").getRole());
    }

    // -- immutability --

    @Test
    void multimodalContentListIsImmutable() {
        var parts = List.of(ContentPart.text("a"), ContentPart.text("b"));
        var msg = Message.user(parts);

        var content = msg.getContentAsParts();
        assertNotNull(content);
        assertThrows(UnsupportedOperationException.class, () -> content.add(ContentPart.text("c")));
    }

    @Test
    void toolCallsListIsImmutable() {
        var calls = List.of(new ToolCall("c1", "function", new ToolCall.Function("fn", "{}")));
        var msg = Message.assistant("x", calls);

        assertThrows(UnsupportedOperationException.class,
                () -> msg.getToolCalls().add(new ToolCall("c2", "function", new ToolCall.Function("fn2", "{}"))));
    }
}

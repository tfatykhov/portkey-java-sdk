package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;

/**
 * A chat message. Supports all roles from the Portkey/OpenAI spec:
 * system, developer, user, assistant, tool, function (deprecated).
 *
 * <p>Instances should be treated as effectively immutable after creation.
 * Use the static factory methods to construct messages.
 *
 * <p>For multimodal user messages, use {@link #user(List)} with {@link ContentPart}s:
 * <pre>{@code
 * Message.user(List.of(
 *     ContentPart.text("What's in this image?"),
 *     ContentPart.imageUrl("https://example.com/photo.jpg")
 * ));
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    private String role;
    private Object content; // String or List<ContentPart>
    private String name;

    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    /** Default constructor for Jackson deserialization only. */
    Message() {}

    Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    Message(String role, List<ContentPart> contentParts) {
        this.role = role;
        this.content = List.copyOf(contentParts);
    }

    // -- factory methods --

    /** System message. */
    public static Message system(String content) {
        return new Message("system", content);
    }

    /** Developer message (newer OpenAI role, auto-converted to system for incompatible providers). */
    public static Message developer(String content) {
        return new Message("developer", content);
    }

    /** User message with text content. */
    public static Message user(String content) {
        return new Message("user", content);
    }

    /** User message with multimodal content parts (text + images). */
    public static Message user(List<ContentPart> parts) {
        return new Message("user", parts);
    }

    /** Assistant message. */
    public static Message assistant(String content) {
        return new Message("assistant", content);
    }

    /** Assistant message with tool calls. */
    public static Message assistant(String content, List<ToolCall> toolCalls) {
        var msg = new Message("assistant", content);
        msg.toolCalls = toolCalls != null ? List.copyOf(toolCalls) : null;
        return msg;
    }

    /** Tool response message. */
    public static Message tool(String content, String toolCallId) {
        var msg = new Message("tool", content);
        msg.toolCallId = toolCallId;
        return msg;
    }

    // -- named participant --

    /** Set optional participant name (differentiates same-role participants). Returns a new copy. */
    public Message withName(String name) {
        var copy = new Message();
        copy.role = this.role;
        copy.content = this.content;
        copy.toolCalls = this.toolCalls;
        copy.toolCallId = this.toolCallId;
        copy.name = name;
        return copy;
    }

    // -- getters --

    public String getRole() { return role; }

    /**
     * Content as raw object. May be a String or List of ContentPart.
     */
    public Object getContent() { return content; }

    /**
     * Content as String (returns null if content is multimodal).
     */
    public String getContentAsText() {
        return content instanceof String s ? s : null;
    }

    /**
     * Content as list of parts (returns null if content is a plain string).
     * The list elements are guaranteed to be {@link ContentPart} instances
     * when constructed via factory methods.
     */
    @SuppressWarnings("unchecked")
    public List<ContentPart> getContentAsParts() {
        return content instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof ContentPart
                ? (List<ContentPart>) list
                : null;
    }

    public String getName() { return name; }
    public List<ToolCall> getToolCalls() { return toolCalls; }
    public String getToolCallId() { return toolCallId; }

    // -- Jackson deserialization setters (package-private) --

    @JsonSetter
    void setRole(String role) { this.role = role; }
    @JsonSetter
    void setContent(Object content) { this.content = content; }
    @JsonSetter
    void setName(String name) { this.name = name; }
    @JsonSetter("tool_calls")
    void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    @JsonSetter("tool_call_id")
    void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    @Override
    public String toString() {
        return "Message{role='%s', content=%s}".formatted(role, content);
    }
}

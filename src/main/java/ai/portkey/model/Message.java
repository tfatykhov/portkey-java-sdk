package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A message in a chat conversation.
 *
 * <p>Roles: "system", "user", "assistant", "tool", "developer".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Message {

    private String role;
    private String content;
    private String name;

    @JsonProperty("tool_call_id")
    private String toolCallId;

    public Message() {}

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static Message system(String content) {
        return new Message("system", content);
    }

    public static Message user(String content) {
        return new Message("user", content);
    }

    public static Message assistant(String content) {
        return new Message("assistant", content);
    }

    // -- getters / setters --

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getToolCallId() { return toolCallId; }
    public void setToolCallId(String toolCallId) { this.toolCallId = toolCallId; }

    @Override
    public String toString() {
        return "Message{role='" + role + "', content='" +
                (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content) + "'}";
    }
}

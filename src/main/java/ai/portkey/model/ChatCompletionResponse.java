package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from POST /v1/chat/completions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {

    private String id;
    private String object;
    private long created;
    private String model;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    private List<Choice> choices;
    private Usage usage;

    // -- getters / setters --

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }

    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getSystemFingerprint() { return systemFingerprint; }
    public void setSystemFingerprint(String systemFingerprint) { this.systemFingerprint = systemFingerprint; }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }

    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    /**
     * Convenience: get the content of the first choice's message.
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            Message msg = choices.get(0).getMessage();
            return msg != null ? msg.getContent() : null;
        }
        return null;
    }

    @Override
    public String toString() {
        return "ChatCompletionResponse{id='" + id + "', model='" + model +
                "', choices=" + (choices != null ? choices.size() : 0) +
                ", usage=" + usage + "}";
    }
}

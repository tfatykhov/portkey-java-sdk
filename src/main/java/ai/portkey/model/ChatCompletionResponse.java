package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from POST /v1/chat/completions.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        String id,
        String object,
        long created,
        String model,
        @JsonProperty("system_fingerprint") String systemFingerprint,
        List<Choice> choices,
        Usage usage
) {
    /**
     * Convenience: content of the first choice's message.
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            var msg = choices.getFirst().message();
            return msg != null ? msg.getContentAsText() : null;
        }
        return null;
    }
}

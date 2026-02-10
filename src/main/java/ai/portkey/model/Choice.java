package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single completion choice.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Choice(
        int index,
        @JsonProperty("finish_reason") String finishReason,
        Message message,
        Object logprobs
) {}

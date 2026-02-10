package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single completion choice.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Choice {

    private int index;

    @JsonProperty("finish_reason")
    private String finishReason;

    private Message message;
    private Object logprobs;

    // -- getters / setters --

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }

    public Object getLogprobs() { return logprobs; }
    public void setLogprobs(Object logprobs) { this.logprobs = logprobs; }

    @Override
    public String toString() {
        return "Choice{index=" + index + ", finishReason='" + finishReason +
                "', message=" + message + "}";
    }
}

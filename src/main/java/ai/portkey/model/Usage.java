package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token usage statistics.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usage {

    @JsonProperty("prompt_tokens")
    private int promptTokens;

    @JsonProperty("completion_tokens")
    private int completionTokens;

    @JsonProperty("total_tokens")
    private int totalTokens;

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }

    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    @Override
    public String toString() {
        return "Usage{prompt=" + promptTokens + ", completion=" + completionTokens +
                ", total=" + totalTokens + "}";
    }
}

package ai.portkey.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Request body for POST /v1/chat/completions.
 *
 * <p>Use the builder: {@code ChatCompletionRequest.builder().model("gpt-4o").addMessage(...).build()}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private Integer n;
    // NOTE: stream and stream_options intentionally excluded from builder.
    // Streaming requires SSE handling not yet implemented in PortkeyClient.
    // Will be added in a future release with proper Flux/Stream support.

    private Object stop;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("logit_bias")
    private Map<String, Integer> logitBias;

    private String user;
    private Boolean logprobs;

    @JsonProperty("top_logprobs")
    private Integer topLogprobs;

    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;

    private Integer seed;
    private List<Map<String, Object>> tools;

    @JsonProperty("tool_choice")
    private Object toolChoice;

    @JsonProperty("parallel_tool_calls")
    private Boolean parallelToolCalls;

    private Map<String, Object> thinking;

    ChatCompletionRequest() {}

    // -- builder --

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ChatCompletionRequest req = new ChatCompletionRequest();
        private final List<Message> messages = new ArrayList<>();

        public Builder model(String model) { req.model = model; return this; }

        public Builder addMessage(Message msg) { messages.add(msg); return this; }
        public Builder addMessage(String role, String content) { messages.add(new Message(role, content)); return this; }
        public Builder addMessages(List<Message> msgs) { messages.addAll(msgs); return this; }

        public Builder temperature(double t) { req.temperature = t; return this; }
        public Builder topP(double p) { req.topP = p; return this; }
        public Builder n(int n) { req.n = n; return this; }
        public Builder stop(Object stop) { req.stop = stop; return this; }
        public Builder maxTokens(int max) { req.maxTokens = max; return this; }
        public Builder presencePenalty(double p) { req.presencePenalty = p; return this; }
        public Builder frequencyPenalty(double f) { req.frequencyPenalty = f; return this; }
        public Builder logitBias(Map<String, Integer> bias) { req.logitBias = bias; return this; }
        public Builder user(String user) { req.user = user; return this; }
        public Builder logprobs(boolean lp) { req.logprobs = lp; return this; }
        public Builder topLogprobs(int tlp) { req.topLogprobs = tlp; return this; }
        public Builder responseFormat(Map<String, Object> fmt) { req.responseFormat = fmt; return this; }
        public Builder seed(int seed) { req.seed = seed; return this; }
        public Builder tools(List<Map<String, Object>> tools) { req.tools = tools; return this; }
        public Builder toolChoice(Object choice) { req.toolChoice = choice; return this; }
        public Builder parallelToolCalls(boolean p) { req.parallelToolCalls = p; return this; }
        public Builder thinking(Map<String, Object> thinking) { req.thinking = thinking; return this; }

        public ChatCompletionRequest build() {
            if (req.model == null || req.model.isEmpty()) {
                throw new IllegalStateException("model is required");
            }
            if (messages.isEmpty()) {
                throw new IllegalStateException("at least one message is required");
            }
            req.messages = List.copyOf(messages);
            if (req.tools != null) {
                req.tools = List.copyOf(req.tools);
            }
            return req;
        }
    }

    // -- getters (unmodifiable) --

    public String getModel() { return model; }
    public List<Message> getMessages() { return messages; }
    public Double getTemperature() { return temperature; }
    public Double getTopP() { return topP; }
    public Integer getN() { return n; }
    public Object getStop() { return stop; }
    public Integer getMaxTokens() { return maxTokens; }
    public Double getPresencePenalty() { return presencePenalty; }
    public Double getFrequencyPenalty() { return frequencyPenalty; }
    public Map<String, Integer> getLogitBias() { return logitBias; }
    public String getUser() { return user; }
    public Boolean getLogprobs() { return logprobs; }
    public Integer getTopLogprobs() { return topLogprobs; }
    public Map<String, Object> getResponseFormat() { return responseFormat; }
    public Integer getSeed() { return seed; }
    public List<Map<String, Object>> getTools() { return tools; }
    public Object getToolChoice() { return toolChoice; }
    public Boolean getParallelToolCalls() { return parallelToolCalls; }
    public Map<String, Object> getThinking() { return thinking; }
}

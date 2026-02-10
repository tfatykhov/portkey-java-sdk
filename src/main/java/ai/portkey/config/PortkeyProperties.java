package ai.portkey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Spring Boot configuration properties for the Portkey client.
 *
 * <pre>{@code
 * portkey:
 *   api-key: pk-...
 *   virtual-key: my-openai-key
 *   base-url: https://api.portkey.ai/v1
 *   timeout: 60s
 * }</pre>
 */
@ConfigurationProperties(prefix = "portkey")
public class PortkeyProperties {

    /** Portkey API key (required). Sent as x-portkey-api-key. */
    private String apiKey;

    /** Virtual key for provider routing. Sent as x-portkey-virtual-key. */
    private String virtualKey;

    /** Provider name for direct auth. Sent as x-portkey-provider. */
    private String provider;

    /** Provider auth token. Sent as Authorization: Bearer. */
    private String providerAuthToken;

    /** Portkey config ID. Sent as x-portkey-config. */
    private String config;

    /** Custom host override. Sent as x-portkey-custom-host. */
    private String customHost;

    /** Base URL. Default: https://api.portkey.ai/v1 */
    private String baseUrl = "https://api.portkey.ai/v1";

    /** HTTP request timeout. Default: 60s */
    private Duration timeout = Duration.ofSeconds(60);

    /** Additional headers to send with every request. */
    private Map<String, String> headers;

    // -- getters and setters --

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getVirtualKey() { return virtualKey; }
    public void setVirtualKey(String virtualKey) { this.virtualKey = virtualKey; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderAuthToken() { return providerAuthToken; }
    public void setProviderAuthToken(String providerAuthToken) { this.providerAuthToken = providerAuthToken; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public String getCustomHost() { return customHost; }
    public void setCustomHost(String customHost) { this.customHost = customHost; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
}

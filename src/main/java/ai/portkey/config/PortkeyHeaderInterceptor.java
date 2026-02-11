package ai.portkey.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Injects Portkey-specific headers into every HTTP request made by Spring AI's OpenAI client.
 *
 * <p>Reads static header values from {@link PortkeyProperties} at construction time.
 * For dynamic per-request headers, extend this class or add a separate interceptor.
 */
public class PortkeyHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final PortkeyProperties properties;

    public PortkeyHeaderInterceptor(PortkeyProperties properties) {
        this.properties = properties;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        var headers = request.getHeaders();

        // Core Portkey header (always set)
        headers.set("x-portkey-api-key", properties.getApiKey());

        // Optional Portkey routing headers
        if (properties.getVirtualKey() != null) {
            headers.set("x-portkey-virtual-key", properties.getVirtualKey());
        }
        if (properties.getProvider() != null) {
            headers.set("x-portkey-provider", properties.getProvider());
        }
        if (properties.getConfig() != null) {
            headers.set("x-portkey-config", properties.getConfig());
        }
        if (properties.getCustomHost() != null) {
            headers.set("x-portkey-custom-host", properties.getCustomHost());
        }

        // Custom headers from properties
        if (properties.getHeaders() != null) {
            properties.getHeaders().forEach(headers::set);
        }

        return execution.execute(request, body);
    }
}

package ai.portkey.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PortkeyHeaderInterceptorTest {

    private PortkeyProperties createProps(String apiKey, String virtualKey,
                                           String provider, String config,
                                           String customHost, Map<String, String> headers) {
        var props = new PortkeyProperties();
        props.setApiKey(apiKey);
        props.setVirtualKey(virtualKey);
        props.setProvider(provider);
        props.setConfig(config);
        props.setCustomHost(customHost);
        props.setHeaders(headers);
        return props;
    }

    @Test
    void injectsApiKeyHeader() throws Exception {
        var props = createProps("pk-test-123", null, null, null, null, null);
        var interceptor = new PortkeyHeaderInterceptor(props);

        var request = new MockClientHttpRequest(HttpMethod.POST, "/chat/completions");
        var execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst("x-portkey-api-key")).isEqualTo("pk-test-123");
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void injectsAllPortkeyHeaders() throws Exception {
        var props = createProps("pk-test", "vk-openai", "openai", "cfg-123", "https://custom.host", null);
        var interceptor = new PortkeyHeaderInterceptor(props);

        var request = new MockClientHttpRequest(HttpMethod.POST, "/chat/completions");
        var execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, new byte[0], execution);

        var headers = request.getHeaders();
        assertThat(headers.getFirst("x-portkey-api-key")).isEqualTo("pk-test");
        assertThat(headers.getFirst("x-portkey-virtual-key")).isEqualTo("vk-openai");
        assertThat(headers.getFirst("x-portkey-provider")).isEqualTo("openai");
        assertThat(headers.getFirst("x-portkey-config")).isEqualTo("cfg-123");
        assertThat(headers.getFirst("x-portkey-custom-host")).isEqualTo("https://custom.host");
    }

    @Test
    void skipsNullOptionalHeaders() throws Exception {
        var props = createProps("pk-test", null, null, null, null, null);
        var interceptor = new PortkeyHeaderInterceptor(props);

        var request = new MockClientHttpRequest(HttpMethod.POST, "/chat/completions");
        var execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, new byte[0], execution);

        var headers = request.getHeaders();
        assertThat(headers.getFirst("x-portkey-api-key")).isEqualTo("pk-test");
        assertThat(headers.containsKey("x-portkey-virtual-key")).isFalse();
        assertThat(headers.containsKey("x-portkey-provider")).isFalse();
        assertThat(headers.containsKey("x-portkey-config")).isFalse();
        assertThat(headers.containsKey("x-portkey-custom-host")).isFalse();
    }

    @Test
    void injectsCustomHeaders() throws Exception {
        var customHeaders = Map.of(
                "x-portkey-trace-id", "trace-abc",
                "x-custom-header", "custom-value"
        );
        var props = createProps("pk-test", null, null, null, null, customHeaders);
        var interceptor = new PortkeyHeaderInterceptor(props);

        var request = new MockClientHttpRequest(HttpMethod.POST, "/chat/completions");
        var execution = mock(ClientHttpRequestExecution.class);

        interceptor.intercept(request, new byte[0], execution);

        var headers = request.getHeaders();
        assertThat(headers.getFirst("x-portkey-trace-id")).isEqualTo("trace-abc");
        assertThat(headers.getFirst("x-custom-header")).isEqualTo("custom-value");
    }
}

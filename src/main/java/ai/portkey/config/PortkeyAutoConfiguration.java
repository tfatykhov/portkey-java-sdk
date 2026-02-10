package ai.portkey.config;

import ai.portkey.client.PortkeyClient;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Spring Boot auto-configuration for the Portkey client.
 *
 * <p>Activates when {@code portkey.api-key} is set. Creates a {@link PortkeyClient}
 * bean backed by Spring's {@link RestClient}.
 *
 * <p>Override with your own {@code @Bean PortkeyClient} to customize.
 */
@AutoConfiguration
@EnableConfigurationProperties(PortkeyProperties.class)
@ConditionalOnProperty(prefix = "portkey", name = "api-key")
public class PortkeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient portkeyRestClient(PortkeyProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeout());
        factory.setReadTimeout(props.getTimeout());

        var builder = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("x-portkey-api-key", props.getApiKey());

        if (props.getVirtualKey() != null) {
            builder.defaultHeader("x-portkey-virtual-key", props.getVirtualKey());
        }
        if (props.getProvider() != null) {
            builder.defaultHeader("x-portkey-provider", props.getProvider());
        }
        if (props.getProviderAuthToken() != null) {
            builder.defaultHeader("Authorization", "Bearer " + props.getProviderAuthToken());
        }
        if (props.getConfig() != null) {
            builder.defaultHeader("x-portkey-config", props.getConfig());
        }
        if (props.getCustomHost() != null) {
            builder.defaultHeader("x-portkey-custom-host", props.getCustomHost());
        }
        if (props.getHeaders() != null) {
            props.getHeaders().forEach(builder::defaultHeader);
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public PortkeyClient portkeyClient(RestClient portkeyRestClient) {
        return new PortkeyClient(portkeyRestClient);
    }
}

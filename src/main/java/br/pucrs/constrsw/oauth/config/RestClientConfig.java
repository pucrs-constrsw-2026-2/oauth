package br.pucrs.constrsw.oauth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class RestClientConfig {

    @Bean
    RestClient keycloakRestClient(RestClient.Builder builder, KeycloakProperties properties) {
        return builder.baseUrl(properties.serverUrl()).build();
    }
}

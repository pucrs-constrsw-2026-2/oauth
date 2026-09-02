package com.seugrupo.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient único, com a base-url do Keycloak já configurada.
 * Injetem esse bean nos services (KeycloakAuthService, KeycloakUserService,
 * KeycloakRoleService) em vez de instanciar um WebClient novo em cada um.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient keycloakWebClient(KeycloakProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}

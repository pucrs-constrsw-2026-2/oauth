package com.seugrupo.oauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Valida o Bearer token localmente via JWKS (issuer-uri configurado no
 * application.yml), sem chamar /userinfo do Keycloak a cada request.
 * Trade-off aceito e documentado no README: revogação antecipada de token
 * só é percebida quando o token expira naturalmente.
 *
 * /login e /refresh-token ficam públicos (é ali que o token é obtido).
 * Todo o resto exige Bearer token válido.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/refresh-token",
                                // Swagger UI: springdoc.swagger-ui.path = /docs.
                                // O /docs apenas redireciona para /swagger-ui/index.html,
                                // entao ambos precisam estar liberados.
                                "/docs",
                                "/docs/**",
                                "/swagger-ui",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/webjars/**",
                                // JSON/YAML do OpenAPI consumido pela propria UI
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                );

        return http.build();
    }
}

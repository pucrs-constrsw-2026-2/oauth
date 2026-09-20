package com.seugrupo.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seugrupo.oauth.exception.ErrorResponse;
import com.seugrupo.oauth.exception.ErrorResponse.ErrorStackEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Valida o Bearer token localmente via JWKS (issuer-uri configurado no
 * application.yml), sem chamar /userinfo do Keycloak a cada request.
 * Trade-off aceito e documentado no README: revogação antecipada de token
 * só é percebida quando o token expira naturalmente.
 *
 * /login e /refresh-token ficam públicos (é ali que o token é obtido).
 * Todo o resto exige Bearer token válido.
 *
 * Erros 401 e 403 gerados pelo Spring Security no nível de filtro retornam
 * o contrato de erro padronizado exigido pela disciplina.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint authEntryPoint = authenticationEntryPoint();
        AccessDeniedHandler accessDenied = accessDeniedHandler();

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
                                "/health",
                                "/actuator",
                                "/actuator/**",
                                "/metrics"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDenied)
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDenied)
                );

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");

            String detail = authException != null && authException.getMessage() != null
                    ? authException.getMessage()
                    : "Credenciais ou access token inválidos.";

            ErrorResponse error = new ErrorResponse(
                    "OA-401",
                    "Credenciais ou access token inválidos.",
                    "OAuthAPI.Auth",
                    List.of(new ErrorStackEntry(detail))
            );
            objectMapper.writeValue(response.getWriter(), error);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            String detail = accessDeniedException != null && accessDeniedException.getMessage() != null
                    ? accessDeniedException.getMessage()
                    : "Access token não concede permissão para acessar esse endpoint ou objeto.";

            ErrorResponse error = new ErrorResponse(
                    "OA-403",
                    "Access token não concede permissão para acessar esse endpoint ou objeto.",
                    "OAuthAPI.Auth",
                    List.of(new ErrorStackEntry(detail))
            );
            objectMapper.writeValue(response.getWriter(), error);
        };
    }
}

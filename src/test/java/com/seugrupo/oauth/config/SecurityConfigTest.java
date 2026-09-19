package com.seugrupo.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(new ObjectMapper());
    }

    @Test
    void authenticationEntryPointRetorna401ComFormatoPadronizado() throws Exception {
        AuthenticationEntryPoint entryPoint = securityConfig.authenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authEx = new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Token invalido");

        entryPoint.commence(request, response, authEx);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getHeader("WWW-Authenticate")).contains("Bearer error=\"invalid_token\"");

        String json = response.getContentAsString();
        assertThat(json).contains("\"error_code\":\"OA-401\"");
        assertThat(json).contains("\"error_description\":\"Credenciais ou access token inválidos.\"");
        assertThat(json).contains("\"error_source\":\"OAuthAPI.Auth\"");
        assertThat(json).contains("\"error_stack\"");
        assertThat(json).contains("\"message\":\"Token invalido\"");
    }

    @Test
    void accessDeniedHandlerRetorna403ComFormatoPadronizado() throws Exception {
        AccessDeniedHandler handler = securityConfig.accessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessEx = new AccessDeniedException("Permissao negada");

        handler.handle(request, response, accessEx);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");

        String json = response.getContentAsString();
        assertThat(json).contains("\"error_code\":\"OA-403\"");
        assertThat(json).contains("\"error_description\":\"Access token não concede permissão para acessar esse endpoint ou objeto.\"");
        assertThat(json).contains("\"error_source\":\"OAuthAPI.Auth\"");
        assertThat(json).contains("\"error_stack\"");
        assertThat(json).contains("\"message\":\"Permissao negada\"");
    }
}

package br.pucrs.constrsw.oauth.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto.ErrorResponseDto;
import br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto.ErrorStackEntryDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Executado quando o Spring Security rejeita a request por falta/invalidez do
 * Bearer token. Devolve o envelope padronizado da API (mesma forma que os
 * outros erros da API) em vez do body padrao do Spring Security.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseDto body = new ErrorResponseDto(
                "401",
                "Access token ausente ou invalido.",
                "OAuthAPI",
                List.of(new ErrorStackEntryDto(authException.getClass().getSimpleName(),
                        authException.getMessage())));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

package br.pucrs.constrsw.oauth.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto.ErrorResponseDto;
import br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto.ErrorStackEntryDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Executado quando um Bearer token valido nao possui permissao para o endpoint
 * (ex.: sem role manage-users). Devolve o envelope padrao com HTTP 403.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponseDto body = new ErrorResponseDto(
                "403",
                "Access token nao concede permissao para acessar esse endpoint ou objeto.",
                "OAuthAPI",
                List.of(new ErrorStackEntryDto(accessDeniedException.getClass().getSimpleName(),
                        accessDeniedException.getMessage())));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

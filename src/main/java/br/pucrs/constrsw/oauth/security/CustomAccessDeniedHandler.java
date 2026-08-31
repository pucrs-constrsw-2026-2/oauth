package br.pucrs.constrsw.oauth.security;

import br.pucrs.constrsw.oauth.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Runs when a valid Bearer token does not grant enough permission for the endpoint
 * (e.g. wrong realm role), producing the API's standard error envelope with 403.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                "403",
                "Access token nao concede permissao para acessar esse endpoint ou objeto.",
                "OAuthAPI",
                List.of(accessDeniedException.toString()));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

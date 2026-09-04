package com.seugrupo.oauth.service;

import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.LoginRequest;
import com.seugrupo.oauth.dto.RefreshTokenRequest;
import com.seugrupo.oauth.dto.TokenResponse;
import com.seugrupo.oauth.exception.KeycloakErrorMapper;
import com.seugrupo.oauth.exception.OAuthApiException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Service
public class KeycloakAuthService {

    private static final String ERROR_SOURCE = "OAuthAPI.Auth";
    private static final Duration TOKEN_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient keycloakWebClient;
    private final KeycloakProperties properties;

    public KeycloakAuthService(WebClient keycloakWebClient, KeycloakProperties properties) {
        this.keycloakWebClient = keycloakWebClient;
        this.properties = properties;
    }

    public TokenResponse login(LoginRequest request) {
        MultiValueMap<String, String> form = clientCredentialsForm();
        form.add("grant_type", "password");
        form.add("username", request.username());
        form.add("password", request.password());
        return requestToken(form);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = clientCredentialsForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", request.refreshToken());
        return requestToken(form);
    }

    private MultiValueMap<String, String> clientCredentialsForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        return form;
    }

    private TokenResponse requestToken(MultiValueMap<String, String> form) {
        try {
            TokenResponse response = keycloakWebClient.post()
                    .uri(properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .timeout(TOKEN_REQUEST_TIMEOUT)
                    .onErrorMap(TimeoutException.class,
                            ex -> upstreamFailure("Tempo limite excedido ao chamar o Keycloak."))
                    .onErrorMap(DecodingException.class,
                            ex -> upstreamFailure("O Keycloak retornou uma resposta inválida."))
                    .block();

            if (!isComplete(response)) {
                throw upstreamFailure("O Keycloak retornou uma resposta vazia ou incompleta.");
            }
            return response;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                    && ex.getResponseBodyAsString().contains("invalid_client")) {
                throw upstreamFailure("O Keycloak rejeitou as credenciais configuradas para o client OAuth.");
            }
            throw KeycloakErrorMapper.map(ex, ERROR_SOURCE);
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException ex) {
            throw upstreamFailure("Não foi possível conectar ao Keycloak.");
        }
    }

    private boolean isComplete(TokenResponse response) {
        return response != null
                && hasText(response.tokenType())
                && hasText(response.accessToken())
                && response.expiresIn() > 0
                && hasText(response.refreshToken())
                && response.refreshExpiresIn() > 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static OAuthApiException upstreamFailure(String message) {
        return new OAuthApiException(
                HttpStatus.BAD_GATEWAY,
                "OA-502",
                ERROR_SOURCE,
                message
        );
    }
}

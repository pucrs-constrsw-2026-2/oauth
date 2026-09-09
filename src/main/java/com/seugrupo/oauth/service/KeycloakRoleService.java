package com.seugrupo.oauth.service;

import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.RoleResponse;
import com.seugrupo.oauth.exception.KeycloakErrorMapper;
import com.seugrupo.oauth.exception.OAuthApiException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class KeycloakRoleService {

    private static final String ERROR_SOURCE = "OAuthAPI.Roles";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient keycloakWebClient;
    private final KeycloakProperties properties;

    public KeycloakRoleService(WebClient keycloakWebClient, KeycloakProperties properties) {
        this.keycloakWebClient = keycloakWebClient;
        this.properties = properties;
    }

    public List<RoleResponse> list(String authorization) {
        return execute(() -> keycloakWebClient.get()
                .uri(properties.getAdminRolesUrl())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(RoleResponse.class)
                .collectList()
                .timeout(REQUEST_TIMEOUT)
                .block());
    }

    public RoleResponse getById(String authorization, String id) {
        return list(authorization).stream()
                .filter(role -> Objects.equals(id, role.id()))
                .findFirst()
                .orElseThrow(() -> new OAuthApiException(
                        HttpStatus.NOT_FOUND,
                        "OA-404",
                        ERROR_SOURCE,
                        "Role não localizada no Keycloak."
                ));
    }

    private <T> T execute(Supplier<T> request) {
        try {
            return request.get();
        } catch (WebClientResponseException ex) {
            throw KeycloakErrorMapper.map(ex, ERROR_SOURCE);
        } catch (WebClientRequestException | DecodingException ex) {
            throw upstreamFailure();
        } catch (RuntimeException ex) {
            if (Exceptions.unwrap(ex) instanceof TimeoutException) {
                throw upstreamFailure();
            }
            throw ex;
        }
    }

    private OAuthApiException upstreamFailure() {
        return new OAuthApiException(
                HttpStatus.BAD_GATEWAY,
                "OA-502",
                ERROR_SOURCE,
                "Falha ao comunicar com o Keycloak."
        );
    }
}

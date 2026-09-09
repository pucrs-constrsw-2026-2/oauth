package com.seugrupo.oauth.service;

import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.CreateUserRequest;
import com.seugrupo.oauth.dto.UpdatePasswordRequest;
import com.seugrupo.oauth.dto.UpdateUserRequest;
import com.seugrupo.oauth.dto.UserResponse;
import com.seugrupo.oauth.exception.KeycloakErrorMapper;
import com.seugrupo.oauth.exception.OAuthApiException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Service
public class KeycloakUserService {

    private static final String ERROR_SOURCE = "OAuthAPI.Users";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient keycloakWebClient;
    private final KeycloakProperties properties;

    public KeycloakUserService(WebClient keycloakWebClient, KeycloakProperties properties) {
        this.keycloakWebClient = keycloakWebClient;
        this.properties = properties;
    }

    public UserResponse create(String authorization, CreateUserRequest request) {
        Map<String, Object> body = Map.of(
                "username", request.username(),
                "email", request.username(),
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.password(),
                        "temporary", false)));

        ResponseEntity<Void> response = execute(() -> keycloakWebClient.post()
                .uri(properties.getAdminUsersUrl())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .timeout(REQUEST_TIMEOUT)
                .block());

        String id = idFrom(response == null ? null : response.getHeaders().getLocation());
        return new UserResponse(id, request.username(), request.firstName(), request.lastName(), true);
    }

    public List<UserResponse> list(String authorization, Boolean enabled) {
        String url = enabled == null
                ? properties.getAdminUsersUrl()
                : UriComponentsBuilder.fromUriString(properties.getAdminUsersUrl())
                .queryParam("enabled", enabled)
                .build()
                .toUriString();

        return execute(() -> keycloakWebClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(UserResponse.class)
                .collectList()
                .timeout(REQUEST_TIMEOUT)
                .block());
    }

    public UserResponse getById(String authorization, String id) {
        return execute(() -> keycloakWebClient.get()
                .uri(properties.getAdminUsersUrl() + "/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .switchIfEmpty(Mono.error(upstreamFailure()))
                .timeout(REQUEST_TIMEOUT)
                .block());
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

    private String idFrom(URI location) {
        if (location == null || location.getPath() == null) {
            throw upstreamFailure();
        }
        String path = location.getPath();
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash == path.length() - 1) {
            throw upstreamFailure();
        }
        return path.substring(slash + 1);
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

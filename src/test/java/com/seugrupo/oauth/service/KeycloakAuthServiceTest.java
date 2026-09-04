package com.seugrupo.oauth.service;

import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.LoginRequest;
import com.seugrupo.oauth.dto.RefreshTokenRequest;
import com.seugrupo.oauth.dto.TokenResponse;
import com.seugrupo.oauth.exception.OAuthApiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakAuthServiceTest {

    private HttpServer server;
    private KeycloakAuthService service;
    private KeycloakProperties properties;
    private final AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();
    private volatile int responseStatus;
    private volatile String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 200;
        responseBody = tokenJson();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleRequest);
        server.start();

        properties = new KeycloakProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRealm("constrsw");
        properties.setClientId("oauth-client");
        properties.setClientSecret("client-secret");
        service = new KeycloakAuthService(WebClient.create(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void loginEnviaPasswordGrantEMapeiaResposta() {
        TokenResponse response = service.login(new LoginRequest("lucas@example.com", "senha segura"));

        CapturedRequest request = capturedRequest.get();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/realms/constrsw/protocol/openid-connect/token");
        assertThat(request.contentType()).startsWith("application/x-www-form-urlencoded");
        assertThat(request.form()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "client_id", "oauth-client",
                "client_secret", "client-secret",
                "grant_type", "password",
                "username", "lucas@example.com",
                "password", "senha segura"
        ));
        assertThat(response).isEqualTo(new TokenResponse("Bearer", "access", 300, "refresh", 1800));
    }

    @Test
    void refreshEnviaSomenteCamposDoRefreshGrant() {
        service.refresh(new RefreshTokenRequest("refresh-value"));

        Map<String, String> form = capturedRequest.get().form();
        assertThat(form).containsExactlyInAnyOrderEntriesOf(Map.of(
                "client_id", "oauth-client",
                "client_secret", "client-secret",
                "grant_type", "refresh_token",
                "refresh_token", "refresh-value"
        ));
        assertThat(form).doesNotContainKeys("username", "password");
    }

    @Test
    void loginMapeiaRejeicaoDoKeycloakPara401SemVazarCorpo() {
        responseStatus = 401;
        responseBody = "{\"error\":\"invalid_grant\",\"error_description\":\"upstream detail\"}";

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "incorreta")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-401");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                    assertThat(ex.getMessage()).doesNotContain("upstream detail", "incorreta");
                });
    }

    @Test
    void refreshPreserva400DoKeycloak() {
        responseStatus = 400;
        responseBody = "{\"error\":\"invalid_grant\"}";

        assertThatThrownBy(() -> service.refresh(new RefreshTokenRequest("expirado")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-400");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                });
    }

    @Test
    void respostaVaziaDoKeycloakNaoProduzFalsoSucesso() {
        responseBody = "";

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "senha")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                });
    }

    @Test
    void respostaIncompletaDoKeycloakNaoProduzFalsoSucesso() {
        responseBody = "{}";

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "senha")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                });
    }

    @Test
    void respostaMalformadaDoKeycloakRetorna502() {
        responseBody = "not-json";

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "senha")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                });
    }

    @Test
    void clientInvalidoEhFalhaDeIntegracaoEnaoCredencialDoUsuario() {
        responseStatus = 401;
        responseBody = "{\"error\":\"invalid_client\"}";

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "senha")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                });
    }

    @Test
    void falhaDeConexaoComKeycloakRetorna502() {
        WebClient unavailableClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new WebClientRequestException(
                        new ConnectException("connection refused"),
                        HttpMethod.POST,
                        URI.create(properties.getTokenUrl()),
                        HttpHeaders.EMPTY
                )))
                .build();
        service = new KeycloakAuthService(unavailableClient, properties);

        assertThatThrownBy(() -> service.login(new LoginRequest("lucas", "senha")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Auth");
                });
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        capturedRequest.set(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.CONTENT_TYPE),
                parseForm(body)
        ));
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private Map<String, String> parseForm(String body) {
        return Arrays.stream(body.split("&"))
                .filter(part -> !part.isBlank())
                .map(part -> part.split("=", 2))
                .collect(Collectors.toMap(
                        part -> decode(part[0]),
                        part -> part.length == 2 ? decode(part[1]) : ""
                ));
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String tokenJson() {
        return """
                {
                  "token_type": "Bearer",
                  "access_token": "access",
                  "expires_in": 300,
                  "refresh_token": "refresh",
                  "refresh_expires_in": 1800,
                  "scope": "openid profile"
                }
                """;
    }

    private record CapturedRequest(
            String method,
            String path,
            String contentType,
            Map<String, String> form
    ) {
    }
}

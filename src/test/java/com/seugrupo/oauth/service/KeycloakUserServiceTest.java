package com.seugrupo.oauth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.CreateUserRequest;
import com.seugrupo.oauth.dto.UserResponse;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakUserServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private HttpServer server;
    private KeycloakUserService service;
    private KeycloakProperties properties;
    private final AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();
    private volatile int responseStatus;
    private volatile String responseBody;
    private volatile String location;

    @BeforeEach
    void setUp() throws IOException {
        responseStatus = 200;
        responseBody = usersJson();
        location = null;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleRequest);
        server.start();

        properties = new KeycloakProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRealm("constrsw");
        service = new KeycloakUserService(WebClient.create(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void createEncaminhaBearerECredencialTemporariaFalse() {
        responseStatus = 201;
        location = properties.getAdminUsersUrl() + "/user-123";

        UserResponse response = service.create("Bearer access", new CreateUserRequest(
                "ana@example.com", "segredo", "Ana", "Silva"));

        CapturedRequest request = capturedRequest.get();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/admin/realms/constrsw/users");
        assertThat(request.authorization()).isEqualTo("Bearer access");
        assertThat(request.json()).containsEntry("username", "ana@example.com")
                .containsEntry("email", "ana@example.com")
                .containsEntry("firstName", "Ana")
                .containsEntry("lastName", "Silva")
                .containsEntry("enabled", true);
        assertThat(request.rawBody()).contains("\"type\":\"password\"")
                .contains("\"value\":\"segredo\"")
                .contains("\"temporary\":false");
        assertThat(response).isEqualTo(
                new UserResponse("user-123", "ana@example.com", "Ana", "Silva", true));
    }

    @Test
    void createSemLocationRetorna502SemDetalheUpstream() {
        responseStatus = 201;

        assertThatThrownBy(() -> service.create("Bearer access", new CreateUserRequest(
                "ana@example.com", "segredo", "Ana", "Silva")))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Users");
                    assertThat(ex.getMessage()).doesNotContain("127.0.0.1", "201");
                });
    }

    @Test
    void listComFiltroEnabledEncaminhaQueryEDecodificaLista() {
        List<UserResponse> response = service.list("Bearer access", true);

        CapturedRequest request = capturedRequest.get();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.path()).isEqualTo("/admin/realms/constrsw/users");
        assertThat(request.query()).isEqualTo("enabled=true");
        assertThat(request.authorization()).isEqualTo("Bearer access");
        assertThat(response).containsExactly(
                new UserResponse("u-1", "ana@example.com", "Ana", "Silva", true),
                new UserResponse("u-2", "bia@example.com", "Bia", "Souza", false));
    }

    @Test
    void listSemFiltroNaoEnviaParametroEnabled() {
        service.list("Bearer access", null);

        assertThat(capturedRequest.get().query()).isNull();
    }

    @Test
    void getByIdDecodificaNomesDoKeycloak() {
        responseBody = """
                {"id":"u-1","username":"ana@example.com","firstName":"Ana",
                 "lastName":"Silva","enabled":true}
                """;

        UserResponse response = service.getById("Bearer access", "u-1");

        assertThat(capturedRequest.get().path())
                .isEqualTo("/admin/realms/constrsw/users/u-1");
        assertThat(response).isEqualTo(
                new UserResponse("u-1", "ana@example.com", "Ana", "Silva", true));
    }

    @Test
    void resposta404ViraErroPadraoSemCorpoUpstream() {
        responseStatus = 404;
        responseBody = "{\"error\":\"not found\",\"detail\":\"internal\"}";

        assertThatThrownBy(() -> service.getById("Bearer access", "missing"))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-404");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Users");
                    assertThat(ex.getMessage()).doesNotContain("internal", "not found");
                });
    }

    @Test
    void respostaMalformadaOuVaziaVira502() {
        responseBody = "not-json";

        assertThatThrownBy(() -> service.getById("Bearer access", "u-1"))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Users");
                });
    }

    @Test
    void falhaDeConexaoVira502() {
        WebClient unavailableClient = WebClient.builder()
                .exchangeFunction(request -> Mono.error(new WebClientRequestException(
                        new ConnectException("connection refused"),
                        HttpMethod.GET,
                        URI.create(properties.getAdminUsersUrl()),
                        HttpHeaders.EMPTY
                )))
                .build();
        service = new KeycloakUserService(unavailableClient, properties);

        assertThatThrownBy(() -> service.list("Bearer access", null))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-502");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Users");
                });
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String rawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        CapturedRequest request = new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                rawBody,
                rawBody.isBlank() ? Map.of() : objectMapper.readValue(
                        rawBody, new TypeReference<Map<String, Object>>() {
                        }));
        capturedRequest.set(request);
        if (location != null) {
            exchange.getResponseHeaders().set(HttpHeaders.LOCATION, location);
        }
        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String usersJson() {
        return """
                [
                  {"id":"u-1","username":"ana@example.com","firstName":"Ana",
                   "lastName":"Silva","enabled":true},
                  {"id":"u-2","username":"bia@example.com","firstName":"Bia",
                   "lastName":"Souza","enabled":false}
                ]
                """;
    }

    private record CapturedRequest(
            String method,
            String path,
            String query,
            String authorization,
            String rawBody,
            Map<String, Object> json
    ) {
    }
}

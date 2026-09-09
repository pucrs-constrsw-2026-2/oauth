package com.seugrupo.oauth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seugrupo.oauth.config.KeycloakProperties;
import com.seugrupo.oauth.dto.RoleResponse;
import com.seugrupo.oauth.exception.OAuthApiException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeycloakRoleServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AtomicReference<CapturedRequest> lastRequest = new AtomicReference<>();
    private final Deque<StubResponse> responses = new ArrayDeque<>();
    private HttpServer server;
    private KeycloakRoleService service;
    private KeycloakProperties properties;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handleRequest);
        server.start();

        properties = new KeycloakProperties();
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setRealm("constrsw");
        service = new KeycloakRoleService(WebClient.create(), properties);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void listUsaGetCaminhoCorretoEBearer() {
        List<RoleResponse> roles = service.list("Bearer access");

        assertThat(lastRequest.get().method()).isEqualTo("GET");
        assertThat(lastRequest.get().path()).isEqualTo("/admin/realms/constrsw/roles");
        assertThat(lastRequest.get().authorization()).isEqualTo("Bearer access");
        assertThat(roles).containsExactly(
                role("r-1", "teacher", "Docente"),
                role("r-2", "student", "Discente"));
    }

    @Test
    void getByIdResolveIdNaListaSemEndpointInventado() {
        RoleResponse role = service.getById("Bearer access", "r-2");

        assertThat(role).isEqualTo(role("r-2", "student", "Discente"));
        assertThat(lastRequest.get().path()).isEqualTo("/admin/realms/constrsw/roles");
    }

    @Test
    void getByIdInexistenteRetorna404Local() {
        assertThatThrownBy(() -> service.getById("Bearer access", "inexistente"))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-404");
                    assertThat(ex.getErrorSource()).isEqualTo("OAuthAPI.Roles");
                });
        assertThat(lastRequest.get().path()).isEqualTo("/admin/realms/constrsw/roles");
    }

    @Test
    void listVaziaRetornaListaVaziaEGetVazioRetorna404() {
        response(200, "[]");
        assertThat(service.list("Bearer access")).isEmpty();

        response(200, "[]");
        assertThatThrownBy(() -> service.getById("Bearer access", "missing"))
                .isInstanceOfSatisfying(OAuthApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getErrorCode()).isEqualTo("OA-404");
                });
    }

    private RoleResponse role(String id, String name, String description) {
        return new RoleResponse(id, name, description, false, false, "constrsw");
    }

    private void response(int status, String body) {
        responses.add(new StubResponse(status, body));
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String rawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        lastRequest.set(new CapturedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestURI().getQuery(),
                exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                rawBody,
                rawBody.isBlank() ? Map.of() : objectMapper.readValue(
                        rawBody, new TypeReference<Map<String, Object>>() {
                        })));

        StubResponse response = responses.isEmpty()
                ? new StubResponse(200, rolesJson())
                : responses.removeFirst();
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(response.status(), bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private String rolesJson() {
        return """
                [
                  {"id":"r-1","name":"teacher","description":"Docente",
                   "composite":false,"clientRole":false,"containerId":"constrsw"},
                  {"id":"r-2","name":"student","description":"Discente",
                   "composite":false,"clientRole":false,"containerId":"constrsw"}
                ]
                """;
    }

    private record StubResponse(int status, String body) {
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

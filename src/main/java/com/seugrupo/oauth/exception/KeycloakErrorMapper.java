package com.seugrupo.oauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Traduz uma resposta de erro vinda do Keycloak (via WebClient) para o
 * formato de erro padronizado da nossa API.
 *
 * Como não há instrução em sentido contrário no enunciado, o error_code
 * repassa o response code do Keycloak (ex: "OA-401" para um 401).
 *
 * Usem esse mapper dentro dos services (KeycloakAuthService,
 * KeycloakUserService, KeycloakRoleService) ao capturar
 * WebClientResponseException, passando o "source" apropriado
 * (ex: "OAuthAPI.Auth", "OAuthAPI.Users", "OAuthAPI.Roles").
 */
public final class KeycloakErrorMapper {

    private KeycloakErrorMapper() {
    }

    public static OAuthApiException map(WebClientResponseException ex, String source) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        String description = switch (status) {
            case UNAUTHORIZED -> "Credenciais ou access token inválidos.";
            case FORBIDDEN -> "Access token não concede permissão para acessar esse endpoint ou objeto.";
            case NOT_FOUND -> "Objeto não localizado no Keycloak.";
            case CONFLICT -> "Conflito: o recurso já existe (ex: username já cadastrado).";
            case BAD_REQUEST -> "Erro na estrutura da chamada ao Keycloak (headers, body etc.).";
            default -> "Erro ao comunicar com o Keycloak.";
        };

        return new OAuthApiException(status, "OA-" + status.value(), source, description);
    }
}

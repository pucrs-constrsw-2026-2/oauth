package com.seugrupo.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centraliza os parâmetros de acesso ao Keycloak (base-url, realm, client-id,
 * client-secret). O client_id/client_secret são FIXOS, vindos de variável de
 * ambiente - decisão registrada no README: o grant "password" pressupõe um
 * client confidencial, então quem chama /login não deve escolher o client.
 *
 * Valores lidos do application.yml, prefixo "keycloak":
 *   keycloak.base-url
 *   keycloak.realm
 *   keycloak.client-id
 *   keycloak.client-secret
 */
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /** URL completa do endpoint de token do Keycloak (login e refresh). */
    public String getTokenUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    /** URL base da Admin API de usuários do realm. */
    public String getAdminUsersUrl() {
        return baseUrl + "/admin/realms/" + realm + "/users";
    }

    /** URL base da Admin API de roles do realm. */
    public String getAdminRolesUrl() {
        return baseUrl + "/admin/realms/" + realm + "/roles";
    }
}

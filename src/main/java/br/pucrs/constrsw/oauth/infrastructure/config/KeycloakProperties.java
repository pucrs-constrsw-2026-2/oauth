package br.pucrs.constrsw.oauth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do Keycloak (URL base, realm, credenciais do client oauth e do
 * admin do master). Preenchida via application.yml / variaveis de ambiente.
 */
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String adminUsername;
    private String adminPassword;

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

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String tokenEndpoint() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String masterTokenEndpoint() {
        return baseUrl + "/realms/master/protocol/openid-connect/token";
    }

    public String issuerUri() {
        return baseUrl + "/realms/" + realm;
    }

    public String usersEndpoint() {
        return baseUrl + "/admin/realms/" + realm + "/users";
    }

    public String userEndpoint(String userId) {
        return usersEndpoint() + "/" + userId;
    }

    public String resetPasswordEndpoint(String userId) {
        return userEndpoint(userId) + "/reset-password";
    }

    public String adminApiBaseUrl() {
        return baseUrl + "/admin/realms/" + realm;
    }
}

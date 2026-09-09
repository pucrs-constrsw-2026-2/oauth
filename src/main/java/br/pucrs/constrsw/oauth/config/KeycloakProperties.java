package br.pucrs.constrsw.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /** Base URL of the Keycloak server, e.g. http://keycloak:8080 (internal Docker network name). */
    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    /** Bootstrap admin of the Keycloak instance (master realm) - used to call the Admin REST API. */
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

    public String issuerUri() {
        return baseUrl + "/realms/" + realm;
    }

    /** Token endpoint of the master realm, where the Keycloak bootstrap admin lives. */
    public String masterTokenEndpoint() {
        return baseUrl + "/realms/master/protocol/openid-connect/token";
    }

    /** Base URL of the Admin REST API for this project's realm, e.g. for /users, /roles. */
    public String adminApiBaseUrl() {
        return baseUrl + "/admin/realms/" + realm;
    }
}

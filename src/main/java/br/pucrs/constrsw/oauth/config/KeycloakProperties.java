package br.pucrs.constrsw.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    /** Base URL of the Keycloak server, e.g. http://keycloak:8080 (internal Docker network name). */
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

    public String tokenEndpoint() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String issuerUri() {
        return baseUrl + "/realms/" + realm;
    }
}

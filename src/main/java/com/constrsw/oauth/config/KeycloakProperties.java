package com.constrsw.oauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracao do cliente Keycloak: URL base, realm e credenciais do client
 * "oauth". Valores populados a partir de application.yml / variaveis de
 * ambiente (KEYCLOAK_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT_ID,
 * KEYCLOAK_CLIENT_SECRET).
 */
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String url;
    private String realm;
    private String clientId;
    private String clientSecret;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String tokenEndpoint() {
        return url + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String usersEndpoint() {
        return url + "/admin/realms/" + realm + "/users";
    }

    public String userEndpoint(String userId) {
        return usersEndpoint() + "/" + userId;
    }

    public String resetPasswordEndpoint(String userId) {
        return userEndpoint(userId) + "/reset-password";
    }
}

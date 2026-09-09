package com.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de PUT /users/{id}. Todos os campos sao opcionais - so os presentes
 * sao repassados ao Keycloak.
 */
public class UserUpdateRequest {

    private String username;

    @JsonProperty("first-name")
    private String firstName;

    @JsonProperty("last-name")
    private String lastName;

    private Boolean enabled;

    public UserUpdateRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

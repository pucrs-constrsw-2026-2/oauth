package com.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta padrao para operacoes sobre usuarios. Campos em kebab-case seguindo
 * o enunciado (a estrategia global de naming ja e KEBAB_CASE, mas anotacoes
 * explicitas deixam o contrato claro na leitura).
 */
public class UserResponse {

    private String id;
    private String username;

    @JsonProperty("first-name")
    private String firstName;

    @JsonProperty("last-name")
    private String lastName;

    private Boolean enabled;

    public UserResponse() {}

    public UserResponse(String id, String username, String firstName, String lastName, Boolean enabled) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

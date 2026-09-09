package com.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload de POST /users. Aceita tanto camelCase (firstName/lastName) quanto
 * kebab-case (first-name/last-name) do enunciado - a estrategia padrao do
 * Jackson e KEBAB_CASE (application.yml), e os aliases abaixo garantem
 * compatibilidade nas duas convencoes.
 */
public class UserCreateRequest {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

    @JsonProperty("first-name")
    private String firstName;

    @JsonProperty("last-name")
    private String lastName;

    public UserCreateRequest() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}

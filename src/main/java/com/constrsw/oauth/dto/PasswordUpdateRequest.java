package com.constrsw.oauth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload de PATCH /users/{id} para atualizacao de senha.
 */
public class PasswordUpdateRequest {

    @NotBlank(message = "password is required")
    private String password;

    public PasswordUpdateRequest() {}

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

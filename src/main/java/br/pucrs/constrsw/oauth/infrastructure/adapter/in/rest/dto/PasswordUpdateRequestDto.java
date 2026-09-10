package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body do PATCH /users/{id} (troca de senha). */
public class PasswordUpdateRequestDto {

    @NotBlank(message = "password is required")
    private String password;

    public PasswordUpdateRequestDto() {}

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

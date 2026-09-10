package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.pucrs.constrsw.oauth.domain.model.NewUser;
import jakarta.validation.constraints.NotBlank;

/** Request body do POST /users (kebab-case first-name/last-name conforme enunciado). */
public class UserCreateRequestDto {

    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "password is required")
    private String password;

    @JsonProperty("first-name")
    private String firstName;

    @JsonProperty("last-name")
    private String lastName;

    public UserCreateRequestDto() {}

    public NewUser toDomain() {
        return new NewUser(username, password, firstName, lastName);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}

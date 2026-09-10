package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.pucrs.constrsw.oauth.domain.model.UserUpdate;

/** Request body do PUT /users/{id}. Todos os campos sao opcionais. */
public class UserUpdateRequestDto {

    private String username;

    @JsonProperty("first-name")
    private String firstName;

    @JsonProperty("last-name")
    private String lastName;

    private Boolean enabled;

    public UserUpdateRequestDto() {}

    public UserUpdate toDomain() {
        return new UserUpdate(username, firstName, lastName, enabled);
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}

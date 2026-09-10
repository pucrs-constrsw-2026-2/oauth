package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.pucrs.constrsw.oauth.domain.model.User;

/** Response de /users e /users/{id} (kebab-case). */
public class UserResponseDto {

    private String id;
    private String username;

    @JsonProperty("first-name")
    private String firstName;

    @JsonProperty("last-name")
    private String lastName;

    private Boolean enabled;

    public UserResponseDto() {}

    public UserResponseDto(String id, String username, String firstName, String lastName, Boolean enabled) {
        this.id = id;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
    }

    public static UserResponseDto fromDomain(User u) {
        return new UserResponseDto(u.getId(), u.getUsername(),
                u.getFirstName(), u.getLastName(), u.isEnabled());
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

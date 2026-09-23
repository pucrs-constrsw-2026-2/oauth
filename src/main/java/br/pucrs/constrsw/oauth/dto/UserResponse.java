package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        @JsonProperty("id")
        String id,

        @JsonProperty("username")
        String username,

        @JsonProperty("first-name")
        @JsonAlias({"first_name", "firstName"})
        String firstName,

        @JsonProperty("last-name")
        @JsonAlias({"last_name", "lastName"})
        String lastName,

        @JsonProperty("enabled")
        Boolean enabled,

        @JsonProperty("role")
        String role,

        @JsonProperty("roles")
        List<String> roles
) {
    public UserResponse(String id, String username, String email, String firstName, String lastName, Boolean enabled) {
        this(id, username != null ? username : email, firstName, lastName, enabled, null, null);
    }

    public UserResponse(String id, String username, String firstName, String lastName, Boolean enabled) {
        this(id, username, firstName, lastName, enabled, null, null);
    }

    public UserResponse(String id, String username, String firstName, String lastName, Boolean enabled, String role) {
        this(id, username, firstName, lastName, enabled, role, role != null ? List.of(role) : null);
    }

    @JsonIgnore
    public String email() {
        return username;
    }
}

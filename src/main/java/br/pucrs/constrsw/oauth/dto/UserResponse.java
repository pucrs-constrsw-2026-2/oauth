package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        Boolean enabled
) {
    public UserResponse(String id, String username, String email, String firstName, String lastName, Boolean enabled) {
        this(id, username != null ? username : email, firstName, lastName, enabled);
    }

    @JsonIgnore
    public String email() {
        return username;
    }
}

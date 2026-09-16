package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateUserRequest(
        @Pattern(
                regexp = "^([!-#-'*+/-9=?A-Z\\^-~]+(\\.[!-#-'*+/-9=?A-Z\\^-~]+)*|\"([!#-\\[\\^-~ \\t]|(\\\\[\\t -~]))+\")@([!-#-'*+/-9=?A-Z\\^-~]+(\\.[!-#-'*+/-9=?A-Z\\^-~]+)*|\\[[\\t -Z\\^-~]*\\])$",
                message = "E-mail inválido - RFC 5322 official standard regular expression to validate email addresses"
        )
        @JsonAlias({"username"})
        String email,

        @JsonProperty("first-name")
        @JsonAlias({"first_name", "firstName"})
        String firstName,

        @JsonProperty("last-name")
        @JsonAlias({"last_name", "lastName"})
        String lastName,

        Boolean enabled
) {}

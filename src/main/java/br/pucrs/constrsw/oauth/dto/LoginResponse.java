package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        Long expiresIn,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("refresh_expires_in")
        @JsonAlias({"referesh_expires_in"})
        Long refreshExpiresIn,

        @JsonProperty("scope")
        String scope
) {
    @JsonProperty("referesh_expires_in")
    public Long refereshExpiresIn() {
        return refreshExpiresIn;
    }
}

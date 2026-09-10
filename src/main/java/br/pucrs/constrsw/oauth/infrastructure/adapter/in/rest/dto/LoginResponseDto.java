package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.pucrs.constrsw.oauth.domain.model.AuthTokens;

/** Response body do POST /login. */
public class LoginResponseDto {

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

    public LoginResponseDto() {}

    public LoginResponseDto(String tokenType, String accessToken, Long expiresIn,
                            String refreshToken, Long refreshExpiresIn) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public static LoginResponseDto fromDomain(AuthTokens t) {
        return new LoginResponseDto(t.getTokenType(), t.getAccessToken(),
                t.getExpiresIn(), t.getRefreshToken(), t.getRefreshExpiresIn());
    }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public Long getRefreshExpiresIn() { return refreshExpiresIn; }
    public void setRefreshExpiresIn(Long refreshExpiresIn) { this.refreshExpiresIn = refreshExpiresIn; }
}

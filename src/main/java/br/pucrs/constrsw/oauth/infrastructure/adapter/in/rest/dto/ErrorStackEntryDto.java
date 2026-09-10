package br.pucrs.constrsw.oauth.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Um frame do error_stack: {type, message}. */
public class ErrorStackEntryDto {

    @JsonProperty("type")
    private String type;

    @JsonProperty("message")
    private String message;

    public ErrorStackEntryDto() {}

    public ErrorStackEntryDto(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

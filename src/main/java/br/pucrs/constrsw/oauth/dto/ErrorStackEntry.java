package br.pucrs.constrsw.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One frame of an {@link ErrorResponse}'s error_stack, matching the assignment's example
 * ("error_stack": [{...}]) - an array of objects rather than plain strings.
 */
public class ErrorStackEntry {

    @JsonProperty("type")
    private String type;

    @JsonProperty("message")
    private String message;

    public ErrorStackEntry() {
    }

    public ErrorStackEntry(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

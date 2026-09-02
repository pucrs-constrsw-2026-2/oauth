package br.pucrs.constrsw.oauth.dto;

import java.util.List;

public class ValidateResponse {
    private boolean allowed;
    private String message;
    private String username;
    private String resource;
    private List<String> roles;

    public ValidateResponse() {}

    public ValidateResponse(boolean allowed, String message, String username, String resource, List<String> roles) {
        this.allowed = allowed;
        this.message = message;
        this.username = username;
        this.resource = resource;
        this.roles = roles;
    }

    public static ValidateResponse allowed(String username, String resource, List<String> roles) {
        return new ValidateResponse(true, "Access granted to resource: " + resource, username, resource, roles);
    }

    public static ValidateResponse forbidden(String message, String username, String resource, List<String> roles) {
        return new ValidateResponse(false, message, username, resource, roles);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}

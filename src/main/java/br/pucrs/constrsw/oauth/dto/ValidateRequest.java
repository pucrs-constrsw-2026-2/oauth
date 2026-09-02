package br.pucrs.constrsw.oauth.dto;

public class ValidateRequest {
    private String resource;

    public ValidateRequest() {}

    public ValidateRequest(String resource) {
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }
}

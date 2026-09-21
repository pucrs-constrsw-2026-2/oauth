package br.pucrs.constrsw.oauth.error;

public class KeycloakServiceException extends RuntimeException {

    private final int status;
    private final String responseBody;

    public KeycloakServiceException(int status, String message, String responseBody) {
        super(message);
        this.status = status;
        this.responseBody = responseBody;
    }

    public int getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}

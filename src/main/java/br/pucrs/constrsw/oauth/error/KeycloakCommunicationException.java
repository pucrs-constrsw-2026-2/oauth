package br.pucrs.constrsw.oauth.error;

public class KeycloakCommunicationException extends RuntimeException {

    public KeycloakCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

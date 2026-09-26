package br.pucrs.constrsw.oauth.error;

public class InvalidLoginRequestException extends RuntimeException {

    public InvalidLoginRequestException(String message) {
        super(message);
    }
}

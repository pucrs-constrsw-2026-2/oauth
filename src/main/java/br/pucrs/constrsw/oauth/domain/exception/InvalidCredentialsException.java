package br.pucrs.constrsw.oauth.domain.exception;

/** Username e/ou password invalidos no login. Mapeada em HTTP 401. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}

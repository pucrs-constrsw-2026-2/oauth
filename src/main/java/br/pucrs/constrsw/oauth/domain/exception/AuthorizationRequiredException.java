package br.pucrs.constrsw.oauth.domain.exception;

/** Bearer token ausente ou invalido. Mapeada em HTTP 401. */
public class AuthorizationRequiredException extends DomainException {

    public AuthorizationRequiredException(String message) {
        super(message);
    }
}

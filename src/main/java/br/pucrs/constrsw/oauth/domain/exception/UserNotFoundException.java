package br.pucrs.constrsw.oauth.domain.exception;

/** Nao existe usuario com esse id. Mapeada em HTTP 404. */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String id) {
        super("User not found: " + id);
    }
}

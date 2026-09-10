package br.pucrs.constrsw.oauth.domain.exception;

/** Ja existe usuario com esse username. Mapeada em HTTP 409. */
public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}

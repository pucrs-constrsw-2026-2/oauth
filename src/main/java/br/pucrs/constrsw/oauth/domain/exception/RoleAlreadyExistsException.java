package br.pucrs.constrsw.oauth.domain.exception;

/** Ja existe role com esse nome. Mapeada em HTTP 409. */
public class RoleAlreadyExistsException extends DomainException {

    public RoleAlreadyExistsException(String name) {
        super("Role already exists: " + name);
    }
}

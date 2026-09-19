package br.pucrs.constrsw.oauth.domain.exception;

/** Nao existe role com esse id. Mapeada em HTTP 404. */
public class RoleNotFoundException extends DomainException {

    public RoleNotFoundException(String id) {
        super("Role not found: " + id);
    }
}

package br.pucrs.constrsw.oauth.domain.exception;

/** Falha da validacao RFC 5322 do e-mail (username). Mapeada em HTTP 400. */
public class InvalidEmailException extends DomainException {

    public InvalidEmailException(String email) {
        super("Invalid e-mail (RFC 5322): " + email);
    }
}

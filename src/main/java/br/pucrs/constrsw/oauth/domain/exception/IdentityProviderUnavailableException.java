package br.pucrs.constrsw.oauth.domain.exception;

/** Falha ao contatar o provedor de identidade externo (Keycloak). Mapeada em HTTP 503. */
public class IdentityProviderUnavailableException extends DomainException {

    public IdentityProviderUnavailableException(String message) {
        super(message);
    }

    public IdentityProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

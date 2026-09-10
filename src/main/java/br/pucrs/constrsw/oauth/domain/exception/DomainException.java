package br.pucrs.constrsw.oauth.domain.exception;

/**
 * Raiz da hierarquia de excecoes do dominio. Independente de frameworks -
 * cada adapter (ex.: adapter/in/rest) traduz para o codigo HTTP apropriado.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

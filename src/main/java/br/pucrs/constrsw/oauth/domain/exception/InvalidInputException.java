package br.pucrs.constrsw.oauth.domain.exception;

/** Entrada semanticamente invalida (ex.: nenhum campo para atualizar). Mapeada em HTTP 400. */
public class InvalidInputException extends DomainException {

    public InvalidInputException(String message) {
        super(message);
    }
}

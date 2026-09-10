package br.pucrs.constrsw.oauth.application.port.in;

public interface DisableUserUseCase {
    void execute(String bearer, String id);
}

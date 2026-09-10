package br.pucrs.constrsw.oauth.application.port.in;

public interface UpdatePasswordUseCase {
    void execute(String bearer, String id, String newPassword);
}

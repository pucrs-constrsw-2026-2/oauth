package br.pucrs.constrsw.oauth.application.port.in;

public interface DeleteRoleUseCase {
    void execute(String bearer, String id);
}

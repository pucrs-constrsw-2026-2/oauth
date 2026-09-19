package br.pucrs.constrsw.oauth.application.port.in;

public interface DetachRoleFromUserUseCase {
    void execute(String bearer, String userId, String roleId);
}

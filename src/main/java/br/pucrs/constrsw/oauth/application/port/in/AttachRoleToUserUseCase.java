package br.pucrs.constrsw.oauth.application.port.in;

public interface AttachRoleToUserUseCase {
    void execute(String bearer, String userId, String roleId);
}

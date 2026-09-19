package br.pucrs.constrsw.oauth.application.usecase;

import org.springframework.stereotype.Service;

import br.pucrs.constrsw.oauth.application.port.in.AttachRoleToUserUseCase;
import br.pucrs.constrsw.oauth.application.port.out.RoleGateway;

@Service
public class AttachRoleToUserService implements AttachRoleToUserUseCase {

    private final RoleGateway roleGateway;

    public AttachRoleToUserService(RoleGateway roleGateway) {
        this.roleGateway = roleGateway;
    }

    @Override
    public void execute(String bearer, String userId, String roleId) {
        roleGateway.assignToUser(bearer, userId, roleId);
    }
}

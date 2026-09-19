package br.pucrs.constrsw.oauth.application.usecase;

import org.springframework.stereotype.Service;

import br.pucrs.constrsw.oauth.application.port.in.DetachRoleFromUserUseCase;
import br.pucrs.constrsw.oauth.application.port.out.RoleGateway;

@Service
public class DetachRoleFromUserService implements DetachRoleFromUserUseCase {

    private final RoleGateway roleGateway;

    public DetachRoleFromUserService(RoleGateway roleGateway) {
        this.roleGateway = roleGateway;
    }

    @Override
    public void execute(String bearer, String userId, String roleId) {
        roleGateway.unassignFromUser(bearer, userId, roleId);
    }
}

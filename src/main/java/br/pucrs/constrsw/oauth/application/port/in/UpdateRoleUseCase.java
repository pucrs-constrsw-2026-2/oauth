package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.RoleUpdate;

/** Usado tanto pelo PUT (atualizacao total) quanto pelo PATCH (parcial) de /roles/{id}. */
public interface UpdateRoleUseCase {
    void execute(String bearer, String id, RoleUpdate update);
}

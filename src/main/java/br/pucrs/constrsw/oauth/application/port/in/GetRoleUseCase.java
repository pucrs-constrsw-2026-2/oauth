package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.Role;

public interface GetRoleUseCase {
    Role execute(String bearer, String id);
}

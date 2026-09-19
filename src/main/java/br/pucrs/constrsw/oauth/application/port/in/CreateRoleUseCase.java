package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.NewRole;
import br.pucrs.constrsw.oauth.domain.model.Role;

public interface CreateRoleUseCase {
    Role execute(String bearer, NewRole newRole);
}

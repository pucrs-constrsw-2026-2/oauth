package br.pucrs.constrsw.oauth.application.port.in;

import java.util.List;

import br.pucrs.constrsw.oauth.domain.model.Role;

public interface ListRolesUseCase {
    List<Role> execute(String bearer, Boolean enabled);
}

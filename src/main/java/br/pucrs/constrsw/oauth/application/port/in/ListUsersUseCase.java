package br.pucrs.constrsw.oauth.application.port.in;

import java.util.List;

import br.pucrs.constrsw.oauth.domain.model.User;

public interface ListUsersUseCase {
    List<User> execute(String bearer, Boolean enabled);
}

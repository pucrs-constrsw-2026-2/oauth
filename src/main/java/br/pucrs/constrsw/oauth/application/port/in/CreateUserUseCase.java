package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.NewUser;
import br.pucrs.constrsw.oauth.domain.model.User;

public interface CreateUserUseCase {
    User execute(String bearer, NewUser newUser);
}

package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.User;

public interface GetUserUseCase {
    User execute(String bearer, String id);
}

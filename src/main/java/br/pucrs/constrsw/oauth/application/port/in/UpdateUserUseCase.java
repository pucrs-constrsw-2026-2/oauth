package br.pucrs.constrsw.oauth.application.port.in;

import br.pucrs.constrsw.oauth.domain.model.UserUpdate;

public interface UpdateUserUseCase {
    void execute(String bearer, String id, UserUpdate update);
}

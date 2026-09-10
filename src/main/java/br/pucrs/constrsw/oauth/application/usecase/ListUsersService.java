package br.pucrs.constrsw.oauth.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import br.pucrs.constrsw.oauth.application.port.in.ListUsersUseCase;
import br.pucrs.constrsw.oauth.application.port.out.UserGateway;
import br.pucrs.constrsw.oauth.domain.model.User;

@Service
public class ListUsersService implements ListUsersUseCase {

    private final UserGateway userGateway;

    public ListUsersService(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    @Override
    public List<User> execute(String bearer, Boolean enabled) {
        return userGateway.list(bearer, enabled);
    }
}

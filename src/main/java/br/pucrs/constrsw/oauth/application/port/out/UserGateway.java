package br.pucrs.constrsw.oauth.application.port.out;

import java.util.List;

import br.pucrs.constrsw.oauth.domain.model.NewUser;
import br.pucrs.constrsw.oauth.domain.model.User;
import br.pucrs.constrsw.oauth.domain.model.UserUpdate;

/**
 * Port de saida: CRUD de usuarios contra o provedor de identidade.
 * O bearer token do chamador e propagado ao provider (proxy de autorizacao),
 * de modo que o provider decide se a operacao pode ser feita.
 */
public interface UserGateway {

    /** Cria o usuario e devolve a versao persistida (com id gerado). */
    User create(String bearer, NewUser newUser);

    /**
     * Lista usuarios. Quando enabled != null, filtra por status.
     */
    List<User> list(String bearer, Boolean enabled);

    User findById(String bearer, String id);

    void update(String bearer, String id, UserUpdate update);

    void updatePassword(String bearer, String id, String newPassword);

    /** Exclusao logica: enabled=false no provider. */
    void disable(String bearer, String id);
}

package br.pucrs.constrsw.oauth.application.port.out;

import java.util.List;

import br.pucrs.constrsw.oauth.domain.model.NewRole;
import br.pucrs.constrsw.oauth.domain.model.Role;
import br.pucrs.constrsw.oauth.domain.model.RoleUpdate;

/**
 * Port de saida: CRUD de roles (realm roles) e atribuicao/remocao de roles
 * a usuarios, contra o provedor de identidade. O bearer token do chamador e
 * propagado ao provider (proxy de autorizacao), de modo que o provider
 * decide se a operacao pode ser feita.
 */
public interface RoleGateway {

    /** Cria o role e devolve a versao persistida (com id gerado). */
    Role create(String bearer, NewRole newRole);

    /** Lista roles. Quando enabled != null, filtra por status. */
    List<Role> list(String bearer, Boolean enabled);

    Role findById(String bearer, String id);

    /** Atualizacao total ou parcial: campos null em {@code update} nao sao alterados. */
    void update(String bearer, String id, RoleUpdate update);

    /** Exclusao logica: enabled=false (via atributo customizado) no provider. */
    void delete(String bearer, String id);

    /** Atribui o role ao usuario (realm role mapping). */
    void assignToUser(String bearer, String userId, String roleId);

    /** Remove a atribuicao do role ao usuario (realm role mapping). */
    void unassignFromUser(String bearer, String userId, String roleId);
}

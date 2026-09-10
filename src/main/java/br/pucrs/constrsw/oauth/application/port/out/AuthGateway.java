package br.pucrs.constrsw.oauth.application.port.out;

import br.pucrs.constrsw.oauth.domain.model.AuthTokens;
import br.pucrs.constrsw.oauth.domain.model.Credentials;

/**
 * Port de saida: qualquer provedor de identidade (Keycloak, Auth0, Okta, ...)
 * implementa este contrato para o caso de uso de login funcionar.
 */
public interface AuthGateway {

    /**
     * Autentica as credenciais no provedor e devolve os tokens emitidos.
     * @throws br.pucrs.constrsw.oauth.domain.exception.InvalidCredentialsException
     *         quando as credenciais nao conferem
     * @throws br.pucrs.constrsw.oauth.domain.exception.IdentityProviderUnavailableException
     *         quando o provedor esta inacessivel ou respondeu de forma inesperada
     */
    AuthTokens authenticate(Credentials credentials);
}

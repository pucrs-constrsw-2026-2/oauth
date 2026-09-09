import { AuthService } from './auth.service';
import { OAuthApiException } from '../common/exceptions/oauth-api.exception';
import { KeycloakClientService } from '../common/keycloak/keycloak-client.service';

describe('AuthService', () => {
  let service: AuthService;
  let keycloak: jest.Mocked<Pick<KeycloakClientService, 'passwordGrant'>>;

  beforeEach(() => {
    keycloak = { passwordGrant: jest.fn() };
    service = new AuthService(keycloak as unknown as KeycloakClientService);
  });

  it('retorna o token quando as credenciais sao validas', async () => {
    const token = {
      token_type: 'Bearer',
      access_token: 'abc',
      expires_in: 600,
      refresh_token: 'def',
      refresh_expires_in: 1800,
    };
    keycloak.passwordGrant.mockResolvedValue(token);

    const result = await service.login({
      username: 'admin@pucrs.br',
      password: 'a12345678',
    });

    expect(result).toEqual(token);
    expect(keycloak.passwordGrant).toHaveBeenCalledWith(
      'admin@pucrs.br',
      'a12345678',
    );
  });

  it('remapeia erro 400 (invalid_grant) do Keycloak para 401 conforme o enunciado', async () => {
    const original = new OAuthApiException(
      400,
      'invalid_grant: Invalid user credentials',
      [
        {
          error_code: '400',
          error_description: 'Invalid user credentials',
          error_source: 'Keycloak',
        },
      ],
    );
    keycloak.passwordGrant.mockRejectedValue(original);

    try {
      await service.login({ username: 'admin@pucrs.br', password: 'errada' });
      throw new Error('deveria ter lancado OAuthApiException');
    } catch (err) {
      const e = err as OAuthApiException;
      expect(e).toBeInstanceOf(OAuthApiException);
      expect(e.getStatus()).toBe(401);
      expect(e.getResponse()).toMatchObject({
        error_code: '401',
        error_description: 'username e/ou password invalidos.',
      });
    }
  });

  it('preserva o status 503 quando o Keycloak esta fora do ar (nao remapeia)', async () => {
    const original = new OAuthApiException(
      503,
      'Nao foi possivel se comunicar com o Keycloak.',
      [{ error_code: '503', error_description: 'timeout', error_source: 'Keycloak' }],
    );
    keycloak.passwordGrant.mockRejectedValue(original);

    await expect(
      service.login({ username: 'a', password: 'b' }),
    ).rejects.toBe(original);
  });

  it('repropaga erros que nao sao OAuthApiException sem alterar', async () => {
    const unexpected = new Error('boom');
    keycloak.passwordGrant.mockRejectedValue(unexpected);

    await expect(service.login({ username: 'a', password: 'b' })).rejects.toBe(
      unexpected,
    );
  });
});

import { KeycloakSettingsService } from '../config';
import { OaException } from '../errors';
import { KeycloakTokenClient } from './keycloak-token.client';

function settingsStub(): KeycloakSettingsService {
  return {
    tokenUrl: 'http://keycloak:8080/realms/constrsw/protocol/openid-connect/token',
    clientId: 'oauth',
    clientSecret: 'super-secret',
  } as unknown as KeycloakSettingsService;
}

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  } as unknown as Response;
}

describe('KeycloakTokenClient', () => {
  let fetchMock: jest.Mock;
  let client: KeycloakTokenClient;

  beforeEach(() => {
    fetchMock = jest.fn();
    global.fetch = fetchMock as unknown as typeof fetch;
    client = new KeycloakTokenClient(settingsStub());
  });

  describe('passwordGrant', () => {
    it('posts grant_type=password with the server client credentials, never the caller-supplied ones', async () => {
      fetchMock.mockResolvedValue(
        jsonResponse(200, {
          token_type: 'Bearer',
          access_token: 'access-token',
          expires_in: 300,
          refresh_token: 'refresh-token',
          refresh_expires_in: 1800,
        }),
      );

      const result = await client.passwordGrant('aluno@pucrs.br', 'secret');

      expect(fetchMock).toHaveBeenCalledTimes(1);
      const [url, init] = fetchMock.mock.calls[0];
      expect(url).toBe(
        'http://keycloak:8080/realms/constrsw/protocol/openid-connect/token',
      );
      const sentBody = new URLSearchParams(init.body as string);
      expect(sentBody.get('grant_type')).toBe('password');
      expect(sentBody.get('client_id')).toBe('oauth');
      expect(sentBody.get('client_secret')).toBe('super-secret');
      expect(sentBody.get('username')).toBe('aluno@pucrs.br');
      expect(sentBody.get('password')).toBe('secret');

      expect(result).toEqual({
        tokenType: 'Bearer',
        accessToken: 'access-token',
        expiresIn: 300,
        refreshToken: 'refresh-token',
        refreshExpiresIn: 1800,
      });
    });

    it('relays the Keycloak error code as error_code on failure', async () => {
      fetchMock.mockResolvedValue(
        jsonResponse(400, {
          error: 'invalid_grant',
          error_description: 'Invalid user credentials',
        }),
      );

      let thrown: OaException | undefined;
      try {
        await client.passwordGrant('aluno@pucrs.br', 'wrong');
      } catch (error) {
        thrown = error as OaException;
      }

      expect(thrown).toBeInstanceOf(OaException);
      expect(thrown?.getStatus()).toBe(401);
      expect(thrown?.toEnvelope().error_code).toBe('invalid_grant');
    });

    it('throws a 401 OaException when Keycloak is unreachable', async () => {
      fetchMock.mockRejectedValue(new Error('connect ECONNREFUSED'));

      let thrown: OaException | undefined;
      try {
        await client.passwordGrant('aluno@pucrs.br', 'secret');
      } catch (error) {
        thrown = error as OaException;
      }

      expect(thrown).toBeInstanceOf(OaException);
      expect(thrown?.getStatus()).toBe(401);
    });
  });
});

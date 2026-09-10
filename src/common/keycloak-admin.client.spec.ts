import { KeycloakSettingsService } from '../config';
import { OaException } from '../errors';
import { KeycloakAdminClient } from './keycloak-admin.client';

const TOKEN_URL =
  'http://keycloak/realms/master/protocol/openid-connect/token';

function settingsWith(
  overrides: Partial<Record<string, unknown>> = {},
): KeycloakSettingsService {
  return {
    adminRealmUrl: 'http://keycloak/admin/realms/constrsw',
    serverUrl: 'http://keycloak',
    clientId: 'oauth',
    adminUser: 'admin',
    adminPassword: 'test-only',
    ...overrides,
  } as KeycloakSettingsService;
}

function jsonResponse(value: unknown, status = 200): Response {
  return new Response(JSON.stringify(value), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

/** Resolves with the rejection so its envelope can be inspected. */
async function rejection(promise: Promise<unknown>): Promise<OaException> {
  try {
    await promise;
  } catch (error) {
    return error as OaException;
  }
  throw new Error('expected the call to reject');
}

describe('KeycloakAdminClient', () => {
  let fetchMock: jest.Spied<typeof fetch>;
  let client: KeycloakAdminClient;

  beforeEach(() => {
    fetchMock = jest.spyOn(global, 'fetch');
    client = new KeycloakAdminClient(settingsWith());
  });

  afterEach(() => {
    fetchMock.mockRestore();
  });

  /** Answers the admin token request; everything else gets `body`. */
  function upstream(body: unknown, tokenBody: unknown = { access_token: 'admin-token', expires_in: 300 }): void {
    fetchMock.mockImplementation((url) =>
      String(url).includes('/protocol/openid-connect/token')
        ? Promise.resolve(jsonResponse(tokenBody))
        : Promise.resolve(jsonResponse(body)),
    );
  }

  describe('admin authentication', () => {
    it('authenticates against the master realm with admin-cli', async () => {
      upstream({});

      await client.request('/users');

      const [url, init] = fetchMock.mock.calls[0];
      expect(url).toBe(TOKEN_URL);
      const body = String((init as RequestInit).body);
      expect(body).toContain('client_id=admin-cli');
      expect(body).toContain('grant_type=password');
    });

    it('sends the admin token, never a caller token, on Admin API calls', async () => {
      upstream({});

      await client.request('/users');

      const [, init] = fetchMock.mock.calls[1];
      expect((init as RequestInit).headers).toMatchObject({
        Authorization: 'Bearer admin-token',
      });
    });

    it('reuses the token instead of authenticating per request', async () => {
      upstream({});

      await client.request('/users');
      await client.request('/roles');
      await client.request('/clients');

      const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
        String(url).includes('/protocol/openid-connect/token'),
      );
      expect(tokenCalls).toHaveLength(1);
    });

    it('authenticates only once for concurrent callers', async () => {
      upstream({});

      await Promise.all([
        client.request('/a'),
        client.request('/b'),
        client.request('/c'),
      ]);

      const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
        String(url).includes('/protocol/openid-connect/token'),
      );
      expect(tokenCalls).toHaveLength(1);
    });

    it('authenticates again once the cached token has expired', async () => {
      upstream({}, { access_token: 'admin-token', expires_in: 60 });
      const start = Date.now();
      const clock = jest.spyOn(Date, 'now').mockReturnValue(start);

      await client.request('/users');
      // Past the lifetime Keycloak reported.
      clock.mockReturnValue(start + 61_000);
      await client.request('/users');

      const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
        String(url).includes('/protocol/openid-connect/token'),
      );
      expect(tokenCalls).toHaveLength(2);
      clock.mockRestore();
    });

    it('refreshes before the reported expiry, not after', async () => {
      upstream({}, { access_token: 'admin-token', expires_in: 60 });
      const start = Date.now();
      const clock = jest.spyOn(Date, 'now').mockReturnValue(start);

      await client.request('/users');
      // Inside Keycloak's window but within the safety margin: a token about
      // to expire must not be handed to a request that is still in flight.
      clock.mockReturnValue(start + 55_000);
      await client.request('/users');

      const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
        String(url).includes('/protocol/openid-connect/token'),
      );
      expect(tokenCalls).toHaveLength(2);
      clock.mockRestore();
    });

    it('authenticates again after the token is invalidated', async () => {
      upstream({});

      await client.request('/users');
      client.invalidateToken();
      await client.request('/users');

      const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
        String(url).includes('/protocol/openid-connect/token'),
      );
      expect(tokenCalls).toHaveLength(2);
    });
  });

  describe('failures reach the caller as OA errors', () => {
    it('reports 503 when the admin credentials are not configured', async () => {
      const bare = new KeycloakAdminClient(
        settingsWith({ adminUser: undefined, adminPassword: undefined }),
      );

      const error = await rejection(bare.request('/users'));

      expect(error.getStatus()).toBe(503);
      expect(error.toEnvelope().error_code).toBe('OA-503');
      expect(fetchMock).not.toHaveBeenCalled();
    });

    it('reports 503 when authentication cannot reach Keycloak', async () => {
      fetchMock.mockRejectedValue(new Error('connect ECONNREFUSED'));

      const error = await rejection(client.request('/users'));

      expect(error.getStatus()).toBe(503);
      expect(error.toEnvelope().error_description).toBe(
        'Keycloak authentication is unavailable.',
      );
    });

    it('reports 502 when Keycloak rejects the admin credentials', async () => {
      fetchMock.mockResolvedValue(jsonResponse({ error: 'invalid_grant' }, 401));

      const error = await rejection(client.request('/users'));

      expect(error.getStatus()).toBe(502);
      expect(error.toEnvelope().error_description).toBe(
        'Keycloak admin authentication failed.',
      );
    });

    it('reports 502 when the token response carries no access_token', async () => {
      fetchMock.mockResolvedValue(jsonResponse({ not_a_token: true }));

      const error = await rejection(client.request('/users'));

      expect(error.getStatus()).toBe(502);
      expect(error.toEnvelope().error_description).toBe(
        'Keycloak did not return an admin access token.',
      );
    });

    it('does not cache a failed authentication', async () => {
      fetchMock.mockRejectedValueOnce(new Error('connect ECONNREFUSED'));
      await rejection(client.request('/users'));

      upstream({});
      await client.request('/users');

      const tokenCalls = fetchMock.mock.calls.filter(([url]) =>
        String(url).includes('/protocol/openid-connect/token'),
      );
      expect(tokenCalls).toHaveLength(2);
    });
  });

  describe('response helpers', () => {
    it('maps a 403 from Keycloak to 403, not 502', async () => {
      const error = await rejection(
        client.expectSuccess(new Response(null, { status: 403 }), 'Denied.'),
      );

      expect(error.getStatus()).toBe(403);
      expect(error.toEnvelope().error_code).toBe('OA-403');
    });

    it('maps any other failure status to 502 with its description', async () => {
      const error = await rejection(
        client.expectSuccess(new Response(null, { status: 500 }), 'Upstream broke.'),
      );

      expect(error.getStatus()).toBe(502);
      expect(error.toEnvelope().error_description).toBe('Upstream broke.');
    });

    it('accepts a successful response without raising', async () => {
      await expect(
        client.expectSuccess(new Response(null, { status: 204 }), 'unused'),
      ).resolves.toBeUndefined();
    });

    it('reports 502 when the body is not valid JSON', async () => {
      const error = await rejection(
        client.json(new Response('<html>gateway error</html>'), 'Could not decode.'),
      );

      expect(error.getStatus()).toBe(502);
      expect(error.toEnvelope().error_description).toBe('Could not decode.');
    });
  });
});

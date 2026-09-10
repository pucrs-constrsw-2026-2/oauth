import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import * as request from 'supertest';

import { AppModule } from './app.module';
import { KeycloakAdminClient, KeycloakTokenVerifierService } from './common';

/**
 * Boots the whole application and drives it over HTTP.
 *
 * Unit tests check each piece against its own mock, which is exactly why they
 * missed that the roles routes and the shared error filter disagreed on where
 * the description lives. These tests assert the bytes that reach the caller.
 */
describe('oauth API (integration)', () => {
  let app: INestApplication;
  const verify = jest.fn();
  /**
   * Only the outermost boundary is faked. The real KeycloakAdminService,
   * roles exceptions and error filter all run, which is the point: mocking
   * the admin service would skip the very seam this suite exists to guard.
   */
  const fetchMock = jest.fn();
  const originalFetch = global.fetch;

  beforeAll(async () => {
    process.env.KEYCLOAK_SERVER_URL = 'http://keycloak:8080';
    process.env.KEYCLOAK_REALM = 'constrsw';
    process.env.KEYCLOAK_CLIENT_ID = 'oauth';
    process.env.KEYCLOAK_CLIENT_SECRET = 'test-secret-not-a-real-one';
    process.env.KEYCLOAK_ADMIN = 'admin';
    process.env.KEYCLOAK_ADMIN_PASSWORD = 'test-only';

    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    })
      .overrideProvider(KeycloakTokenVerifierService)
      .useValue({ verify })
      .compile();

    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    global.fetch = originalFetch;
    await app?.close();
  });

  beforeEach(() => {
    verify.mockReset();
    fetchMock.mockReset();
    global.fetch = fetchMock as unknown as typeof fetch;
    // The admin client caches its token across requests, which is the point in
    // production but would leak between tests here.
    app.get(KeycloakAdminClient).invalidateToken();
  });

  /** Every error response must carry the four contract fields. */
  function expectOaEnvelope(body: unknown): Record<string, unknown> {
    expect(body).toEqual(
      expect.objectContaining({
        error_code: expect.any(String),
        error_description: expect.any(String),
        error_source: 'OAuthAPI',
        error_stack: expect.any(Array),
      }),
    );
    return body as Record<string, unknown>;
  }

  describe('health', () => {
    it('answers without authentication', async () => {
      const response = await request(app.getHttpServer()).get('/health');

      expect(response.status).toBe(200);
      expect(response.body).toMatchObject({ status: 'ok' });
    });
  });

  describe('the OA envelope is uniform across the app', () => {
    it('formats an unknown route, which no feature module handles', async () => {
      const response = await request(app.getHttpServer()).get('/does-not-exist');

      expect(response.status).toBe(404);
      const body = expectOaEnvelope(response.body);
      expect(body.error_code).toBe('OA-404');
    });

    it('formats a missing Authorization header on a protected route', async () => {
      const response = await request(app.getHttpServer()).get('/roles');

      expect(response.status).toBe(401);
      const body = expectOaEnvelope(response.body);
      expect(body.error_code).toBe('OA-401');
      expect(verify).not.toHaveBeenCalled();
    });

    it('formats a token Keycloak rejects', async () => {
      verify.mockResolvedValue(null);

      const response = await request(app.getHttpServer())
        .get('/roles')
        .set('Authorization', 'Bearer expired-token');

      expect(response.status).toBe(401);
      expect(expectOaEnvelope(response.body).error_code).toBe('OA-401');
    });

    it('answers 503, not 401, when Keycloak cannot be reached', async () => {
      verify.mockRejectedValue(new Error('connect ECONNREFUSED'));

      const response = await request(app.getHttpServer())
        .get('/roles')
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(503);
      expectOaEnvelope(response.body);
    });
  });

  describe('users routes', () => {
    const USER_ID = '11111111-1111-4111-8111-111111111111';
    const ADMIN_BASE = 'http://keycloak:8080/admin/realms/constrsw';

    function administrator(): void {
      verify.mockResolvedValue({
        sub: 'admin-sub',
        raw: { resource_access: { oauth: { roles: ['administrator'] } } },
      });
    }

    const json = (body: unknown, status = 200, headers: Record<string, string> = {}) =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'content-type': 'application/json', ...headers },
      });

    const storedUser = {
      id: USER_ID,
      username: 'aluno@pucrs.br',
      firstName: 'Ana',
      lastName: 'Silva',
      enabled: true,
    };

    /** Answers the admin token, then delegates by URL. */
    function upstream(routes: (url: string, init?: RequestInit) => Response | undefined): void {
      fetchMock.mockImplementation((url: string, init?: RequestInit) => {
        if (url.includes('/protocol/openid-connect/token')) {
          return Promise.resolve(json({ access_token: 'admin-token', expires_in: 300 }));
        }
        const answer = routes(url, init);
        return answer
          ? Promise.resolve(answer)
          : Promise.reject(new Error(`unexpected upstream call: ${url}`));
      });
    }

    it('creates a user and answers 201 with the hyphenated fields', async () => {
      administrator();
      upstream((url, init) => {
        if (url === `${ADMIN_BASE}/users` && init?.method === 'POST') {
          return new Response(null, {
            status: 201,
            headers: { location: `${ADMIN_BASE}/users/${USER_ID}` },
          });
        }
        if (url === `${ADMIN_BASE}/users/${USER_ID}`) return json(storedUser);
        return undefined;
      });

      const response = await request(app.getHttpServer())
        .post('/users')
        .set('Authorization', 'Bearer valid-token')
        .send({
          username: 'aluno@pucrs.br',
          password: 'segredo',
          'first-name': 'Ana',
          'last-name': 'Silva',
        });

      expect(response.status).toBe(201);
      expect(response.body).toEqual({
        id: USER_ID,
        username: 'aluno@pucrs.br',
        'first-name': 'Ana',
        'last-name': 'Silva',
        enabled: true,
      });
    });

    it('answers 409 in the OA envelope when the username is taken', async () => {
      administrator();
      upstream((url, init) =>
        url === `${ADMIN_BASE}/users` && init?.method === 'POST'
          ? json({ errorMessage: 'User exists' }, 409)
          : undefined,
      );

      const response = await request(app.getHttpServer())
        .post('/users')
        .set('Authorization', 'Bearer valid-token')
        .send({ username: 'aluno@pucrs.br', password: 'segredo' });

      expect(response.status).toBe(409);
      expect(expectOaEnvelope(response.body).error_code).toBe('OA-409');
    });

    it('answers 400 for an invalid e-mail without calling Keycloak', async () => {
      administrator();
      upstream(() => undefined);

      const response = await request(app.getHttpServer())
        .post('/users')
        .set('Authorization', 'Bearer valid-token')
        .send({ username: 'nao-e-email', password: 'segredo' });

      expect(response.status).toBe(400);
      expect(expectOaEnvelope(response.body).error_code).toBe('OA-400');
    });

    it('lists only enabled users when no query string is given', async () => {
      administrator();
      let requested = '';
      upstream((url) => {
        requested = url;
        return json([storedUser]);
      });

      const response = await request(app.getHttpServer())
        .get('/users')
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(200);
      expect(requested).toBe(`${ADMIN_BASE}/users?enabled=true`);
    });

    it('answers 404 in the OA envelope for an unknown id', async () => {
      administrator();
      upstream(() => json({}, 404));

      const response = await request(app.getHttpServer())
        .get(`/users/${USER_ID}`)
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(404);
      expect(expectOaEnvelope(response.body).error_code).toBe('OA-404');
    });

    it('disables rather than deleting, answering 204 with no body', async () => {
      administrator();
      const methods: string[] = [];
      upstream((url, init) => {
        methods.push(`${init?.method ?? 'GET'} ${url}`);
        if (url === `${ADMIN_BASE}/users/${USER_ID}` && init?.method === 'PUT') {
          return new Response(null, { status: 204 });
        }
        if (url === `${ADMIN_BASE}/users/${USER_ID}`) return json(storedUser);
        return undefined;
      });

      const response = await request(app.getHttpServer())
        .delete(`/users/${USER_ID}`)
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(204);
      expect(response.body).toEqual({});
      expect(methods.some((call) => call.startsWith('DELETE'))).toBe(false);
    });

    it('refuses a caller without the administrator role', async () => {
      verify.mockResolvedValue({
        sub: 'student-sub',
        raw: { resource_access: { oauth: { roles: ['student'] } } },
      });

      const response = await request(app.getHttpServer())
        .get('/users')
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(403);
      expect(expectOaEnvelope(response.body).error_code).toBe('OA-403');
    });

    it('does not shadow the roles routes that live under /users/:id', async () => {
      // POST /users/:id/roles belongs to the roles module. Registering a
      // /users controller must not swallow it.
      administrator();
      let assigned = false;
      upstream((url, init) => {
        if (url.includes('/role-mappings/clients/')) {
          assigned = true;
          return new Response(null, { status: 204 });
        }
        if (url.includes('/clients?clientId=')) {
          return json([{ id: 'client-uuid', clientId: 'oauth' }]);
        }
        if (url.endsWith('/clients/client-uuid/roles')) {
          return json([
            { id: 'role-uuid', name: 'professor', clientRole: true, containerId: 'client-uuid' },
          ]);
        }
        if (url === `${ADMIN_BASE}/users/${USER_ID}`) return json(storedUser);
        return undefined;
      });

      const response = await request(app.getHttpServer())
        .post(`/users/${USER_ID}/roles`)
        .set('Authorization', 'Bearer valid-token')
        .send({ roleName: 'professor' });

      expect(response.status).toBeLessThan(400);
      expect(assigned).toBe(true);
    });
  });

  describe('roles routes reach the caller with a real description', () => {
    /** A caller Keycloak accepts, carrying the administrator client role. */
    function administrator(): void {
      verify.mockResolvedValue({
        sub: 'admin-sub',
        raw: { resource_access: { oauth: { roles: ['administrator'] } } },
      });
    }

    it('relays the real upstream description, not the exception class name', async () => {
      administrator();
      // The Admin API is unreachable, so the real service raises its own
      // upstream failure. This is the regression guard: while the roles track
      // and the error filter disagreed on where the description lives, the
      // body read "Role Api Exception" instead of the message below.
      fetchMock.mockRejectedValue(new Error('connect ECONNREFUSED'));

      const response = await request(app.getHttpServer())
        .get('/roles')
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(503);
      const body = expectOaEnvelope(response.body);
      expect(body.error_code).toBe('OA-503');
      expect(body.error_description).toBe('Keycloak authentication is unavailable.');
      expect(body.error_description).not.toMatch(/Exception/i);
    });

    it('refuses a caller without the administrator role', async () => {
      verify.mockResolvedValue({
        sub: 'student-sub',
        raw: { resource_access: { oauth: { roles: ['student'] } } },
      });

      const response = await request(app.getHttpServer())
        .get('/roles')
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(403);
      expect(expectOaEnvelope(response.body).error_code).toBe('OA-403');
    });

    it('lets an administrator through to Keycloak and back', async () => {
      administrator();
      // Routed by URL rather than call order: the admin service fetches a
      // fresh admin token before *each* Admin API call, so a positional mock
      // silently feeds the wrong body to the wrong request.
      const json = (body: unknown) =>
        new Response(JSON.stringify(body), {
          status: 200,
          headers: { 'content-type': 'application/json' },
        });
      fetchMock.mockImplementation((url: string) => {
        if (url.includes('/protocol/openid-connect/token')) {
          return Promise.resolve(json({ access_token: 'admin-token' }));
        }
        if (url.includes('/clients?clientId=')) {
          return Promise.resolve(json([{ id: 'client-uuid', clientId: 'oauth' }]));
        }
        if (url.includes('/clients/client-uuid/roles')) {
          return Promise.resolve(json([{ id: 'role-uuid', name: 'professor' }]));
        }
        return Promise.reject(new Error(`unexpected upstream call: ${url}`));
      });

      const response = await request(app.getHttpServer())
        .get('/roles')
        .set('Authorization', 'Bearer valid-token');

      expect(response.status).toBe(200);
      expect(response.body).toEqual([{ id: 'role-uuid', name: 'professor' }]);
      // Never the caller's token: Admin API calls authenticate as the service.
      const authHeaders = fetchMock.mock.calls
        .map(([, init]) => (init as RequestInit | undefined)?.headers)
        .filter(Boolean)
        .map((h) => JSON.stringify(h));
      expect(authHeaders.join(' ')).not.toContain('valid-token');
    });
  });
});

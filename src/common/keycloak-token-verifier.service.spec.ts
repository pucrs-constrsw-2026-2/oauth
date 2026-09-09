import { Logger } from '@nestjs/common';

import { KeycloakSettingsService } from '../config';
import { KeycloakTokenVerifierService } from './keycloak-token-verifier.service';

const USER_INFO_URL =
  'http://keycloak:8080/realms/constrsw/protocol/openid-connect/userinfo';

function verifierWithFetch(
  fetchImpl: jest.Mock,
): KeycloakTokenVerifierService {
  global.fetch = fetchImpl as unknown as typeof fetch;

  return new KeycloakTokenVerifierService({
    userInfoUrl: USER_INFO_URL,
  } as KeycloakSettingsService);
}

function jsonResponse(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    statusText: String(status),
    json: async () => body,
  } as Response;
}

describe('KeycloakTokenVerifierService', () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    // The upstream-failure cases log on purpose; keep the test output readable.
    jest.spyOn(Logger.prototype, 'error').mockImplementation(() => undefined);
  });

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('calls the UserInfo endpoint with the caller token as Bearer', async () => {
    const fetchMock = jest
      .fn()
      .mockResolvedValue(jsonResponse(200, { sub: 'abc' }));
    const verifier = verifierWithFetch(fetchMock);

    await verifier.verify('the-token');

    expect(fetchMock).toHaveBeenCalledWith(USER_INFO_URL, {
      method: 'GET',
      headers: { Authorization: 'Bearer the-token' },
      signal: expect.any(AbortSignal),
    });
  });

  it('bounds the call with a timeout, so a hung Keycloak cannot stall requests', async () => {
    const fetchMock = jest
      .fn()
      .mockResolvedValue(jsonResponse(200, { sub: 'abc' }));
    const verifier = verifierWithFetch(fetchMock);

    await verifier.verify('the-token');

    const { signal } = fetchMock.mock.calls[0][1] as { signal: AbortSignal };
    expect(signal).toBeInstanceOf(AbortSignal);
    expect(signal.aborted).toBe(false);
  });

  it('propagates an aborted request so the guard can answer 503', async () => {
    const abortError = new DOMException('The operation was aborted.', 'TimeoutError');
    const verifier = verifierWithFetch(jest.fn().mockRejectedValue(abortError));

    await expect(verifier.verify('the-token')).rejects.toThrow(/aborted/);
  });

  it('maps the UserInfo payload onto the caller', async () => {
    const verifier = verifierWithFetch(
      jest.fn().mockResolvedValue(
        jsonResponse(200, {
          sub: 'abc-123',
          email: 'aluno@pucrs.br',
          preferred_username: 'aluno@pucrs.br',
          name: 'Aluno',
        }),
      ),
    );

    const user = await verifier.verify('the-token');

    expect(user).toMatchObject({
      sub: 'abc-123',
      email: 'aluno@pucrs.br',
      preferredUsername: 'aluno@pucrs.br',
      name: 'Aluno',
    });
  });

  it('keeps the full payload so later stories can read extra claims', async () => {
    const verifier = verifierWithFetch(
      jest
        .fn()
        .mockResolvedValue(
          jsonResponse(200, { sub: 'abc', email_verified: true }),
        ),
    );

    const user = await verifier.verify('the-token');

    expect(user?.raw).toEqual({ sub: 'abc', email_verified: true });
  });

  it.each([401, 403])(
    'returns null when Keycloak rejects the token with %i',
    async (status) => {
      const verifier = verifierWithFetch(
        jest.fn().mockResolvedValue(jsonResponse(status, {})),
      );

      await expect(verifier.verify('bad-token')).resolves.toBeNull();
    },
  );

  it.each([500, 502, 503])(
    'throws on an upstream failure (%i) instead of reporting an invalid token',
    async (status) => {
      const verifier = verifierWithFetch(
        jest.fn().mockResolvedValue(jsonResponse(status, {})),
      );

      await expect(verifier.verify('the-token')).rejects.toThrow(
        /UserInfo request failed/,
      );
    },
  );

  it('throws when the network call itself fails', async () => {
    const verifier = verifierWithFetch(
      jest.fn().mockRejectedValue(new Error('connect ECONNREFUSED')),
    );

    await expect(verifier.verify('the-token')).rejects.toThrow(
      /ECONNREFUSED/,
    );
  });

  it('throws when the response has no "sub"', async () => {
    const verifier = verifierWithFetch(
      jest.fn().mockResolvedValue(jsonResponse(200, { email: 'a@b.c' })),
    );

    await expect(verifier.verify('the-token')).rejects.toThrow(/missing "sub"/);
  });
});

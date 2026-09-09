import {
  ExecutionContext,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';

import {
  AuthenticatedRequest,
  BearerAuthGuard,
  extractBearerToken,
} from './bearer-auth.guard';
import {
  AuthenticatedUser,
  KeycloakTokenVerifierService,
} from './keycloak-token-verifier.service';

function contextWith(
  authorization?: string,
): { context: ExecutionContext; request: AuthenticatedRequest } {
  const request = { headers: { authorization } } as AuthenticatedRequest;
  const context = {
    switchToHttp: () => ({ getRequest: () => request }),
  } as unknown as ExecutionContext;

  return { context, request };
}

const caller: AuthenticatedUser = {
  sub: 'e1b0c9a4-0000-4000-8000-000000000001',
  email: 'aluno@pucrs.br',
  preferredUsername: 'aluno@pucrs.br',
  raw: {},
};

describe('extractBearerToken', () => {
  it('extracts the token from a well-formed header', () => {
    expect(extractBearerToken('Bearer abc.def.ghi')).toBe('abc.def.ghi');
  });

  it('accepts the scheme in any case (RFC 7235)', () => {
    expect(extractBearerToken('bearer abc')).toBe('abc');
    expect(extractBearerToken('BEARER abc')).toBe('abc');
  });

  it('tolerates surrounding and repeated whitespace', () => {
    expect(extractBearerToken('  Bearer   abc  ')).toBe('abc');
  });

  it.each([
    ['header absent', undefined],
    ['empty header', ''],
    ['scheme only', 'Bearer'],
    ['scheme with no credentials', 'Bearer '],
    ['wrong scheme', 'Basic abc'],
    ['token without a scheme', 'abc.def.ghi'],
    ['extra parts', 'Bearer abc def'],
  ])('returns null for %s', (_label, header) => {
    expect(extractBearerToken(header as string | undefined)).toBeNull();
  });
});

describe('BearerAuthGuard', () => {
  let verify: jest.Mock;
  let guard: BearerAuthGuard;

  beforeEach(() => {
    verify = jest.fn();
    guard = new BearerAuthGuard({
      verify,
    } as unknown as KeycloakTokenVerifierService);
  });

  it('allows the request and attaches the caller when the token is valid', async () => {
    verify.mockResolvedValue(caller);
    const { context, request } = contextWith('Bearer valid-token');

    await expect(guard.canActivate(context)).resolves.toBe(true);
    expect(verify).toHaveBeenCalledWith('valid-token');
    expect(request.user).toEqual(caller);
  });

  it('rejects with 401 when the Authorization header is missing', async () => {
    const { context } = contextWith(undefined);

    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
    expect(verify).not.toHaveBeenCalled();
  });

  it('rejects with 401 when the header is malformed', async () => {
    const { context } = contextWith('Basic dXNlcjpwYXNz');

    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
    expect(verify).not.toHaveBeenCalled();
  });

  it('rejects with 401 when Keycloak does not recognise the token', async () => {
    verify.mockResolvedValue(null);
    const { context, request } = contextWith('Bearer expired-token');

    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      UnauthorizedException,
    );
    expect(request.user).toBeUndefined();
  });

  it('answers 503, not 401, when Keycloak itself is unreachable', async () => {
    verify.mockRejectedValue(new Error('connect ECONNREFUSED'));
    const { context } = contextWith('Bearer valid-token');

    // A caller with a perfectly good token must not be told to log in again
    // because our dependency is down.
    await expect(guard.canActivate(context)).rejects.toBeInstanceOf(
      ServiceUnavailableException,
    );
  });

  it('never decides permissions itself — a verified caller always passes', async () => {
    verify.mockResolvedValue(caller);
    const { context } = contextWith('Bearer token-without-any-role');

    // 403 belongs to Keycloak, on the Admin API call the route makes.
    await expect(guard.canActivate(context)).resolves.toBe(true);
  });
});

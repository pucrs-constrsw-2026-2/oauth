import type { AuthenticatedRequest } from '../common';
import { OaErrorMapper, OaException } from '../errors';
import { AuthzController } from './authz.controller';
import { KeycloakAuthorizationClient } from './keycloak-authorization.client';

function requestWithToken(token: string): AuthenticatedRequest {
  return {
    headers: { authorization: `Bearer ${token}` },
  } as unknown as AuthenticatedRequest;
}

describe('AuthzController', () => {
  let checkPermission: jest.Mock;
  let controller: AuthzController;

  beforeEach(() => {
    checkPermission = jest.fn();
    controller = new AuthzController({
      checkPermission,
    } as unknown as KeycloakAuthorizationClient);
  });

  it('returns 200 (empty body) when Keycloak permits access, forwarding the caller token and resource', async () => {
    checkPermission.mockResolvedValue(true);

    const response = await controller.validate(requestWithToken('caller-token'), {
      resource: 'lessons',
    });

    expect(response).toEqual({});
    expect(checkPermission).toHaveBeenCalledWith('caller-token', 'lessons');
  });

  it('throws a 403 OA envelope when Keycloak denies access', async () => {
    checkPermission.mockResolvedValue(false);

    let thrown: OaException | undefined;
    try {
      await controller.validate(requestWithToken('caller-token'), { resource: 'students' });
    } catch (error) {
      thrown = error as OaException;
    }

    expect(thrown).toBeInstanceOf(OaException);
    const { status, body } = OaErrorMapper.toResponse(thrown);
    expect(status).toBe(403);
    expect(body.error_source).toBe('OAuthAPI');
  });

  it('rejects an unknown resource name with 400 before calling Keycloak', async () => {
    await expect(
      controller.validate(requestWithToken('caller-token'), { resource: 'invoices' }),
    ).rejects.toBeInstanceOf(OaException);

    expect(checkPermission).not.toHaveBeenCalled();
  });

  it('rejects a malformed body with 400 before calling Keycloak', async () => {
    await expect(
      controller.validate(requestWithToken('caller-token'), {}),
    ).rejects.toBeInstanceOf(OaException);

    expect(checkPermission).not.toHaveBeenCalled();
  });
});

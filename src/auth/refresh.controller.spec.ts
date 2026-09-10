import type { Request } from 'express';

import { OaException } from '../errors';
import { KeycloakTokenClient } from './keycloak-token.client';
import { RefreshController } from './refresh.controller';

function requestWithContentType(contentType: string): Request {
  return { headers: { 'content-type': contentType } } as unknown as Request;
}

describe('RefreshController', () => {
  let refreshGrant: jest.Mock;
  let controller: RefreshController;

  beforeEach(() => {
    refreshGrant = jest.fn();
    controller = new RefreshController({
      refreshGrant,
    } as unknown as KeycloakTokenClient);
  });

  it('returns 200 with the same token field set as login on success', async () => {
    refreshGrant.mockResolvedValue({
      tokenType: 'Bearer',
      accessToken: 'new-access-token',
      expiresIn: 300,
      refreshToken: 'new-refresh-token',
      refreshExpiresIn: 1800,
    });

    const response = await controller.refresh(
      requestWithContentType('application/x-www-form-urlencoded'),
      { refresh_token: 'old-refresh-token' },
    );

    expect(refreshGrant).toHaveBeenCalledWith('old-refresh-token');
    expect(response).toEqual({
      token_type: 'Bearer',
      access_token: 'new-access-token',
      expires_in: 300,
      refresh_token: 'new-refresh-token',
      referesh_expires_in: 1800,
    });
  });

  it('succeeds on multipart/form-data too', async () => {
    refreshGrant.mockResolvedValue({
      tokenType: 'Bearer',
      accessToken: 'new-access-token',
      expiresIn: 300,
      refreshToken: 'new-refresh-token',
    });

    await expect(
      controller.refresh(requestWithContentType('multipart/form-data; boundary=x'), {
        refresh_token: 'old-refresh-token',
      }),
    ).resolves.toMatchObject({ access_token: 'new-access-token' });
  });

  it('rejects a missing refresh_token with OA-400 before calling Keycloak', async () => {
    await expect(
      controller.refresh(requestWithContentType('application/x-www-form-urlencoded'), {}),
    ).rejects.toBeInstanceOf(OaException);

    expect(refreshGrant).not.toHaveBeenCalled();
  });

  it('rejects an unsupported content-type with OA-400 before calling Keycloak', async () => {
    await expect(
      controller.refresh(requestWithContentType('application/json'), {
        refresh_token: 'old-refresh-token',
      }),
    ).rejects.toBeInstanceOf(OaException);

    expect(refreshGrant).not.toHaveBeenCalled();
  });

  it('propagates the 401 Keycloak rejects on an invalid/expired refresh token', async () => {
    refreshGrant.mockRejectedValue(
      new OaException(401, 'invalid_grant', 'Token is not active'),
    );

    await expect(
      controller.refresh(requestWithContentType('application/x-www-form-urlencoded'), {
        refresh_token: 'expired-refresh-token',
      }),
    ).rejects.toBeInstanceOf(OaException);
  });
});

import type { Request } from 'express';

import { OaException } from '../errors';
import { LoginController } from './login.controller';
import { KeycloakTokenClient } from './keycloak-token.client';

function requestWithContentType(contentType: string): Request {
  return { headers: { 'content-type': contentType } } as unknown as Request;
}

describe('LoginController', () => {
  let passwordGrant: jest.Mock;
  let controller: LoginController;

  beforeEach(() => {
    passwordGrant = jest.fn();
    controller = new LoginController({
      passwordGrant,
    } as unknown as KeycloakTokenClient);
  });

  it('returns 200 with the mapped token fields on urlencoded success', async () => {
    passwordGrant.mockResolvedValue({
      tokenType: 'Bearer',
      accessToken: 'access-token',
      expiresIn: 300,
      refreshToken: 'refresh-token',
      refreshExpiresIn: 1800,
    });

    const response = await controller.login(
      requestWithContentType('application/x-www-form-urlencoded'),
      { username: 'aluno@pucrs.br', password: 'secret' },
    );

    expect(passwordGrant).toHaveBeenCalledWith('aluno@pucrs.br', 'secret');
    expect(response).toEqual({
      token_type: 'Bearer',
      access_token: 'access-token',
      expires_in: 300,
      refresh_token: 'refresh-token',
      referesh_expires_in: 1800,
    });
  });

  it('succeeds on multipart/form-data too', async () => {
    passwordGrant.mockResolvedValue({
      tokenType: 'Bearer',
      accessToken: 'access-token',
      expiresIn: 300,
      refreshToken: 'refresh-token',
    });

    await expect(
      controller.login(requestWithContentType('multipart/form-data; boundary=x'), {
        username: 'aluno@pucrs.br',
        password: 'secret',
      }),
    ).resolves.toMatchObject({ access_token: 'access-token' });
  });

  it('ignores brief-shaped extra fields (client_id, grant_type) and still succeeds using env credentials', async () => {
    passwordGrant.mockResolvedValue({
      tokenType: 'Bearer',
      accessToken: 'access-token',
      expiresIn: 300,
      refreshToken: 'refresh-token',
    });

    await controller.login(requestWithContentType('application/x-www-form-urlencoded'), {
      username: 'aluno@pucrs.br',
      password: 'secret',
      client_id: 'attacker-supplied-client',
      grant_type: 'password',
    });

    expect(passwordGrant).toHaveBeenCalledWith('aluno@pucrs.br', 'secret');
  });

  it('rejects an unsupported content-type with OA-400 before calling Keycloak', async () => {
    await expect(
      controller.login(requestWithContentType('application/json'), {
        username: 'aluno@pucrs.br',
        password: 'secret',
      }),
    ).rejects.toBeInstanceOf(OaException);

    expect(passwordGrant).not.toHaveBeenCalled();
  });

  it('rejects a missing password with OA-400 before calling Keycloak', async () => {
    await expect(
      controller.login(requestWithContentType('application/x-www-form-urlencoded'), {
        username: 'aluno@pucrs.br',
      }),
    ).rejects.toBeInstanceOf(OaException);

    expect(passwordGrant).not.toHaveBeenCalled();
  });

  it('propagates the 401 Keycloak rejects on bad credentials', async () => {
    passwordGrant.mockRejectedValue(
      new OaException(401, 'invalid_grant', 'Invalid user credentials'),
    );

    await expect(
      controller.login(requestWithContentType('application/x-www-form-urlencoded'), {
        username: 'aluno@pucrs.br',
        password: 'wrong',
      }),
    ).rejects.toBeInstanceOf(OaException);
  });
});

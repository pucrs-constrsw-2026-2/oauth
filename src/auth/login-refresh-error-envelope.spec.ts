import { HttpStatus } from '@nestjs/common';
import type { Request } from 'express';

import { OaErrorMapper } from '../errors';
import { KeycloakTokenClient } from './keycloak-token.client';
import { LoginController } from './login.controller';
import { RefreshController } from './refresh.controller';

/**
 * Story 3.2 — confirms /login and /refresh failures actually round-trip
 * through the Story 3.1 OA mapper into the full envelope, not just "some
 * exception type". This is what the global filter (registered in main.ts)
 * would serialize on the wire.
 */
function requestWithContentType(contentType: string): Request {
  return { headers: { 'content-type': contentType } } as unknown as Request;
}

describe('OA envelope on /login and /refresh failures (story 3.2)', () => {
  describe('POST /login', () => {
    let controller: LoginController;
    let passwordGrant: jest.Mock;

    beforeEach(() => {
      passwordGrant = jest.fn();
      controller = new LoginController({ passwordGrant } as unknown as KeycloakTokenClient);
    });

    it('400 bad structure uses the OA envelope with a local OA-400 code and an array error_stack', async () => {
      const failure = await controller
        .login(requestWithContentType('application/x-www-form-urlencoded'), {
          username: 'aluno@pucrs.br',
        })
        .catch((error: unknown) => error);

      const { status, body } = OaErrorMapper.toResponse(failure);

      expect(status).toBe(HttpStatus.BAD_REQUEST);
      expect(body.error_code).toBe('OA-400');
      expect(body.error_source).toBe('OAuthAPI');
      expect(Array.isArray(body.error_stack)).toBe(true);
      expect(body.error_stack.every((entry) => typeof entry === 'object')).toBe(true);
    });

    it('401 bad credentials uses the OA envelope and relays the Keycloak error_code', async () => {
      passwordGrant.mockRejectedValue(
        OaErrorMapper.fromKeycloak(HttpStatus.UNAUTHORIZED, 'Invalid user credentials', {
          error: 'invalid_grant',
          error_description: 'Invalid user credentials',
        }),
      );

      const failure = await controller
        .login(requestWithContentType('application/x-www-form-urlencoded'), {
          username: 'aluno@pucrs.br',
          password: 'wrong',
        })
        .catch((error: unknown) => error);

      const { status, body } = OaErrorMapper.toResponse(failure);

      expect(status).toBe(HttpStatus.UNAUTHORIZED);
      expect(body.error_code).toBe('invalid_grant');
      expect(body.error_source).toBe('OAuthAPI');
    });
  });

  describe('POST /refresh', () => {
    let controller: RefreshController;
    let refreshGrant: jest.Mock;

    beforeEach(() => {
      refreshGrant = jest.fn();
      controller = new RefreshController({ refreshGrant } as unknown as KeycloakTokenClient);
    });

    it('400 bad structure uses the OA envelope with an array error_stack', async () => {
      const failure = await controller
        .refresh(requestWithContentType('application/x-www-form-urlencoded'), {})
        .catch((error: unknown) => error);

      const { status, body } = OaErrorMapper.toResponse(failure);

      expect(status).toBe(HttpStatus.BAD_REQUEST);
      expect(body.error_code).toBe('OA-400');
      expect(Array.isArray(body.error_stack)).toBe(true);
    });

    it('401 invalid/expired refresh uses the OA envelope, relaying Keycloak with the upstream cause in error_stack', async () => {
      refreshGrant.mockRejectedValue(
        OaErrorMapper.fromKeycloak(HttpStatus.UNAUTHORIZED, 'Token is not active', {
          error: 'invalid_grant',
          error_description: 'Token is not active',
        }),
      );

      const failure = await controller
        .refresh(requestWithContentType('application/x-www-form-urlencoded'), {
          refresh_token: 'expired-refresh-token',
        })
        .catch((error: unknown) => error);

      const { status, body } = OaErrorMapper.toResponse(failure);

      expect(status).toBe(HttpStatus.UNAUTHORIZED);
      expect(body.error_code).toBe('invalid_grant');
      expect(body.error_stack[0]).toMatchObject({
        source: 'keycloak',
        error: 'invalid_grant',
      });
    });
  });

  it('success paths remain unwrapped — the OA envelope is error-only', async () => {
    const passwordGrant = jest.fn().mockResolvedValue({
      tokenType: 'Bearer',
      accessToken: 'access-token',
      expiresIn: 300,
      refreshToken: 'refresh-token',
    });
    const controller = new LoginController({ passwordGrant } as unknown as KeycloakTokenClient);

    const response = await controller.login(
      requestWithContentType('application/x-www-form-urlencoded'),
      { username: 'aluno@pucrs.br', password: 'secret' },
    );

    expect(response).not.toHaveProperty('error_code');
    expect(response).toMatchObject({ token_type: 'Bearer' });
  });
});

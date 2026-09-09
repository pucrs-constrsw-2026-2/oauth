import { KeycloakTokenResult } from './keycloak-token.client';

/**
 * Wire shape for `/login` and `/refresh` success responses. `referesh_expires_in`
 * keeps the brief's spelling verbatim — do not "fix" it (SPEC constraint).
 */
export interface TokenResponseBody {
  readonly token_type: string;
  readonly access_token: string;
  readonly expires_in: number;
  readonly refresh_token: string;
  readonly referesh_expires_in?: number;
}

export function toTokenResponseBody(result: KeycloakTokenResult): TokenResponseBody {
  return {
    token_type: result.tokenType,
    access_token: result.accessToken,
    expires_in: result.expiresIn,
    refresh_token: result.refreshToken,
    ...(result.refreshExpiresIn !== undefined
      ? { referesh_expires_in: result.refreshExpiresIn }
      : {}),
  };
}

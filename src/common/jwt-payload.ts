/**
 * Reads the JWT payload without verifying the signature. The Bearer guard
 * already asked Keycloak UserInfo whether the token is valid; this only
 * recovers claims UserInfo does not return (notably `resource_access`).
 */
export function decodeJwtPayload(
  accessToken: string,
): Record<string, unknown> {
  const parts = accessToken.split('.');
  if (parts.length < 2 || parts[1].length === 0) {
    return {};
  }

  try {
    const json = Buffer.from(parts[1], 'base64url').toString('utf8');
    const payload: unknown = JSON.parse(json);
    if (typeof payload !== 'object' || payload === null) {
      return {};
    }
    return payload as Record<string, unknown>;
  } catch {
    return {};
  }
}

/**
 * Client/realm role claims live on the access token. UserInfo typically omits
 * them, so `/users` and `/roles` would 403 every caller — including
 * `admin@pucrs.br` — if we only inspected the UserInfo body.
 */
export function roleClaimsFromAccessToken(
  accessToken: string,
): Record<string, unknown> {
  const payload = decodeJwtPayload(accessToken);
  const claims: Record<string, unknown> = {};
  if ('resource_access' in payload) {
    claims.resource_access = payload.resource_access;
  }
  if ('realm_access' in payload) {
    claims.realm_access = payload.realm_access;
  }
  return claims;
}

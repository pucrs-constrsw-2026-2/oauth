import { decodeJwtPayload, roleClaimsFromAccessToken } from './jwt-payload';

function jwtWithPayload(payload: unknown): string {
  const header = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString(
    'base64url',
  );
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url');
  return `${header}.${body}.sig`;
}

describe('decodeJwtPayload', () => {
  it('returns the JSON payload of a three-part JWT', () => {
    expect(decodeJwtPayload(jwtWithPayload({ sub: 'abc', aud: 'oauth' }))).toEqual({
      sub: 'abc',
      aud: 'oauth',
    });
  });

  it('returns {} for a non-JWT string', () => {
    expect(decodeJwtPayload('not-a-jwt')).toEqual({});
  });

  it('returns {} for truncated or invalid base64', () => {
    expect(decodeJwtPayload('a.@@@.b')).toEqual({});
  });
});

describe('roleClaimsFromAccessToken', () => {
  it('copies resource_access and realm_access off the access token', () => {
    const token = jwtWithPayload({
      sub: 'user-1',
      resource_access: { oauth: { roles: ['administrator'] } },
      realm_access: { roles: ['offline_access'] },
    });

    expect(roleClaimsFromAccessToken(token)).toEqual({
      resource_access: { oauth: { roles: ['administrator'] } },
      realm_access: { roles: ['offline_access'] },
    });
  });

  it('omits role keys that the token does not carry', () => {
    expect(roleClaimsFromAccessToken(jwtWithPayload({ sub: 'user-1' }))).toEqual(
      {},
    );
  });
});

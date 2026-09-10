import { ForbiddenException } from '@nestjs/common';

import { AdministratorRoleGuard } from './administrator-role.guard';

describe('AdministratorRoleGuard', () => {
  const guard = new AdministratorRoleGuard();

  it('allows the administrator client role', () => {
    expect(() => guard.canActivate(contextWithClaims({
      resource_access: { oauth: { roles: ['administrator'] } },
    }))).not.toThrow();
  });

  it('rejects authenticated users without administrator', () => {
    expect(() => guard.canActivate(contextWithClaims({
      resource_access: { oauth: { roles: ['professor'] } },
    }))).toThrow(ForbiddenException);
  });

  it('rejects a realm administrator without the oauth client role', () => {
    expect(() => guard.canActivate(contextWithClaims({
      realm_access: { roles: ['administrator'] },
    }))).toThrow(ForbiddenException);
  });
});

function contextWithClaims(raw: Record<string, unknown>): any {
  return {
    switchToHttp: () => ({
      getRequest: () => ({ user: { raw } }),
    }),
  };
}
import {
  CanActivate,
  ExecutionContext,
  ForbiddenException,
  Injectable,
} from '@nestjs/common';

import { AuthenticatedRequest } from './bearer-auth.guard';

@Injectable()
export class AdministratorRoleGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    const claims = request.user?.raw ?? {};
    const resourceAccess = claims.resource_access;
    const clientRoles =
      typeof resourceAccess === 'object' && resourceAccess !== null
        ? (resourceAccess as Record<string, unknown>).oauth
        : undefined;
    const roles =
      typeof clientRoles === 'object' && clientRoles !== null
        ? readRoles(clientRoles)
        : [];

    if (!roles.includes('administrator')) {
      throw new ForbiddenException(
        'The administrator role is required to manage roles and assignments.',
      );
    }

    return true;
  }
}

function readRoles(value: unknown): string[] {
  if (typeof value !== 'object' || value === null) {
    return [];
  }
  const roles = (value as Record<string, unknown>).roles;
  return Array.isArray(roles)
    ? roles.filter((role): role is string => typeof role === 'string')
    : [];
}
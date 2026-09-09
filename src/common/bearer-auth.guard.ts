import {
  CanActivate,
  ExecutionContext,
  Injectable,
  ServiceUnavailableException,
  UnauthorizedException,
} from '@nestjs/common';
import type { Request } from 'express';

import {
  AuthenticatedUser,
  KeycloakTokenVerifierService,
} from './keycloak-token-verifier.service';

/** Request with the caller resolved by the guard. */
export interface AuthenticatedRequest extends Request {
  user?: AuthenticatedUser;
}

/**
 * Requires `Authorization: Bearer <token>` and verifies it against Keycloak.
 *
 * Only answers the "who is calling" question — a missing, malformed or
 * rejected token is a `401`. Deciding whether that caller may perform the
 * operation is left to Keycloak, which answers `403` on the Admin API call the
 * route makes; the guard deliberately holds no local permission matrix.
 *
 * Exceptions thrown here are the standard Nest ones, so the uniform error
 * envelope from Story 3.1 will format them once its filter is registered.
 */
@Injectable()
export class BearerAuthGuard implements CanActivate {
  constructor(private readonly verifier: KeycloakTokenVerifierService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const request = context.switchToHttp().getRequest<AuthenticatedRequest>();
    const token = extractBearerToken(request.headers.authorization);

    if (!token) {
      throw new UnauthorizedException(
        'Missing or malformed Authorization header. Expected "Bearer <access_token>".',
      );
    }

    let user: AuthenticatedUser | null;
    try {
      user = await this.verifier.verify(token);
    } catch {
      // Keycloak unreachable: the caller may well be authorized, so answering
      // 401 would be a lie that sends them to re-authenticate for nothing.
      throw new ServiceUnavailableException(
        'Could not verify the access token because Keycloak is unavailable.',
      );
    }

    if (!user) {
      throw new UnauthorizedException('Access token is invalid or expired.');
    }

    request.user = user;
    return true;
  }
}

/**
 * Accepts the scheme case-insensitively, as RFC 7235 requires, and rejects a
 * header with no credentials or extra parts.
 */
export function extractBearerToken(
  authorizationHeader: string | undefined,
): string | null {
  if (!authorizationHeader) {
    return null;
  }

  const parts = authorizationHeader.trim().split(/\s+/);
  if (parts.length !== 2 || parts[0].toLowerCase() !== 'bearer') {
    return null;
  }

  return parts[1].length > 0 ? parts[1] : null;
}

import { Body, Controller, HttpCode, HttpStatus, Post, Req, UseGuards } from '@nestjs/common';

import { AuthenticatedRequest, BearerAuthGuard, extractBearerToken } from '../common';
import { OaErrorMapper } from '../errors';
import { KeycloakAuthorizationClient } from './keycloak-authorization.client';
import { requireAuthzResourceName } from './validate-request.util';

/**
 * `POST /authz/validate` (Story 6.5, CAP-6). Decides nothing itself: the
 * Bearer guard answers "who is calling" (401 on a missing/invalid token) and
 * Keycloak Authorization Services answers "may they access this resource"
 * (200 permit / 403 deny). No local role→resource table exists here — see
 * `keycloak-authz.md` for the realm-derived matrix this route's outcomes are
 * expected to match.
 */
@Controller('authz')
export class AuthzController {
  constructor(private readonly authorization: KeycloakAuthorizationClient) {}

  @Post('validate')
  @HttpCode(HttpStatus.OK)
  @UseGuards(BearerAuthGuard)
  async validate(
    @Req() request: AuthenticatedRequest,
    @Body() body: unknown,
  ): Promise<Record<string, never>> {
    const resource = requireAuthzResourceName(body);
    // The guard already verified this token via UserInfo; re-extract the raw
    // string because Authorization Services needs the token itself, not the
    // resolved caller.
    const accessToken = extractBearerToken(request.headers.authorization) as string;

    const permitted = await this.authorization.checkPermission(accessToken, resource);
    if (!permitted) {
      throw OaErrorMapper.forbidden(
        `This token does not grant access to resource "${resource}".`,
        { resource },
      );
    }

    return {};
  }
}

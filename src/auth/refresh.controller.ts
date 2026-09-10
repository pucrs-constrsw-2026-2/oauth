import {
  Body,
  Controller,
  HttpCode,
  HttpStatus,
  Post,
  Req,
  UseInterceptors,
} from '@nestjs/common';
import { AnyFilesInterceptor } from '@nestjs/platform-express';
import type { Request } from 'express';

import { assertSupportedFormContentType, requireFormField } from './form-body.util';
import { KeycloakTokenClient } from './keycloak-token.client';
import { toTokenResponseBody, TokenResponseBody } from './token-response.mapper';

/**
 * `POST /refresh` — trades a valid `refresh_token` for a new token pair
 * without re-entering the password (Story 2.2, CAP-7). Same content-type,
 * success status, and response shape as `/login`.
 */
@Controller()
export class RefreshController {
  constructor(private readonly tokenClient: KeycloakTokenClient) {}

  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  @UseInterceptors(AnyFilesInterceptor())
  async refresh(
    @Req() request: Request,
    @Body() body: unknown,
  ): Promise<TokenResponseBody> {
    assertSupportedFormContentType(request);
    const refreshToken = requireFormField(body, 'refresh_token');

    const result = await this.tokenClient.refreshGrant(refreshToken);
    return toTokenResponseBody(result);
  }
}

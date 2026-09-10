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
 * `POST /login` — exchanges username/password for Keycloak tokens (Story 2.1,
 * CAP-1). Client credentials are never taken from the request even if the
 * caller sends `client_id` / `grant_type` (the T1 brief lists them in the
 * form) — those fields are accepted and silently ignored.
 */
@Controller()
export class LoginController {
  constructor(private readonly tokenClient: KeycloakTokenClient) {}

  @Post('login')
  @HttpCode(HttpStatus.OK)
  @UseInterceptors(AnyFilesInterceptor())
  async login(
    @Req() request: Request,
    @Body() body: unknown,
  ): Promise<TokenResponseBody> {
    assertSupportedFormContentType(request);
    const username = requireFormField(body, 'username');
    const password = requireFormField(body, 'password');

    const result = await this.tokenClient.passwordGrant(username, password);
    return toTokenResponseBody(result);
  }
}

import { Body, Controller, Post, Req, Res } from '@nestjs/common';
import { ApiTags } from '@nestjs/swagger';
import type { Request, Response } from 'express';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';

@ApiTags('auth')
@Controller('v1/auth')
export class AuthController {
  constructor(private readonly auth: AuthService) {}

  @Post('login')
  async login(@Body() input: LoginDto, @Res({ passthrough: true }) response: Response) {
    const tokens = await this.auth.login(input.username, input.password);
    this.setSessionCookie(response, tokens);
    return { token_type: tokens.token_type, expires_in: tokens.expires_in, refresh_expires_in: tokens.refresh_expires_in };
  }

  @Post('refresh')
  async refresh(@Req() request: Request, @Res({ passthrough: true }) response: Response) {
    const rawSession = request.cookies?.[process.env.SESSION_COOKIE_NAME ?? 'closed_cras_session'];
    const session = typeof rawSession === 'string' ? JSON.parse(rawSession) as { refresh_token?: string } : undefined;
    const refreshToken = session?.refresh_token;
    const tokens = await this.auth.refresh(refreshToken ?? '');
    this.setSessionCookie(response, tokens);
    return { token_type: tokens.token_type, expires_in: tokens.expires_in, refresh_expires_in: tokens.refresh_expires_in };
  }

  @Post('logout')
  logout(@Res({ passthrough: true }) response: Response) {
    response.clearCookie(process.env.SESSION_COOKIE_NAME ?? 'closed_cras_session');
    return { status: 'signed_out' };
  }

  private setSessionCookie(response: Response, tokens: { access_token: string; refresh_token?: string }) {
    response.cookie(process.env.SESSION_COOKIE_NAME ?? 'closed_cras_session', JSON.stringify({ access_token: tokens.access_token, refresh_token: tokens.refresh_token }), {
      httpOnly: true,
      secure: process.env.COOKIE_SECURE === 'true',
      sameSite: (process.env.COOKIE_SAME_SITE ?? 'lax') as 'lax' | 'strict' | 'none',
    });
  }
}
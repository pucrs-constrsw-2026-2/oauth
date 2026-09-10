import { Body, Controller, Post, Req, Res } from "@nestjs/common";
import { ConfigService } from "@nestjs/config";
import { ApiTags } from "@nestjs/swagger";
import type { Request, Response } from "express";
import { AuthService } from "./auth.service";
import { LoginDto } from "./dto/login.dto";
import type {
  AuthResponse,
  LogoutResponse,
} from "./interfaces/auth-response.interface";

@ApiTags("auth")
@Controller("v1/auth")
export class AuthController {
  constructor(
    private readonly auth: AuthService,
    private readonly config: ConfigService,
  ) {}

  @Post("login")
  async login(
    @Body() input: LoginDto,
    @Res({ passthrough: true }) response: Response,
  ): Promise<AuthResponse> {
    const tokens = await this.auth.login(input.username, input.password);
    this.setSessionCookie(response, tokens);
    return this.toAuthResponse(tokens);
  }

  @Post("refresh")
  async refresh(
    @Req() request: Request,
    @Res({ passthrough: true }) response: Response,
  ): Promise<AuthResponse> {
    const rawSession =
      request.cookies?.[this.config.getOrThrow<string>("SESSION_COOKIE_NAME")];
    const session =
      typeof rawSession === "string"
        ? (JSON.parse(rawSession) as { refresh_token?: string })
        : undefined;
    const refreshToken = session?.refresh_token;
    const tokens = await this.auth.refresh(refreshToken ?? "");
    this.setSessionCookie(response, tokens);
    return this.toAuthResponse(tokens);
  }

  @Post("logout")
  logout(@Res({ passthrough: true }) response: Response): LogoutResponse {
    response.clearCookie(this.config.getOrThrow<string>("SESSION_COOKIE_NAME"));
    return { status: "signed_out" };
  }

  private toAuthResponse({
    token_type,
    expires_in,
    refresh_expires_in,
  }: AuthResponse): AuthResponse {
    return { token_type, expires_in, refresh_expires_in };
  }

  private setSessionCookie(
    response: Response,
    tokens: { access_token: string; refresh_token?: string },
  ) {
    response.cookie(
      this.config.getOrThrow<string>("SESSION_COOKIE_NAME"),
      JSON.stringify({
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
      }),
      {
        httpOnly: true,
        secure: this.config.getOrThrow<string>("COOKIE_SECURE") === "true",
        sameSite: this.config.getOrThrow<"lax" | "strict" | "none">(
          "COOKIE_SAME_SITE",
        ),
      },
    );
  }
}

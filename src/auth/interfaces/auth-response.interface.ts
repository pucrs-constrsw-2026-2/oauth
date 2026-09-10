import type { TokenResponse } from "../keycloak.client";

export interface AuthResponse extends Pick<
  TokenResponse,
  "token_type" | "expires_in" | "refresh_expires_in"
> {}

export interface LogoutResponse {
  status: "signed_out";
}

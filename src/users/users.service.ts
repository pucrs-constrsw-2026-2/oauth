import { Injectable, NotFoundException } from "@nestjs/common";
import { KeycloakClient, KeycloakUser } from "../auth/keycloak.client";
import { KeycloakDependencyError } from "../common/errors";

export interface UserResponse {
  id: string;
  username: string;
  "first-name": string;
  "last-name": string;
  enabled: boolean;
}

@Injectable()
export class UsersService {
  constructor(private readonly keycloak: KeycloakClient) {}

  async list(accessToken: string, enabled?: boolean): Promise<UserResponse[]> {
    const users = await this.keycloak.listUsers(accessToken, enabled);
    return users
      .filter((user) => !user.username.startsWith("service-account-"))
      .map((user) => this.toResponse(user));
  }

  async get(accessToken: string, id: string): Promise<UserResponse> {
    const user = await this.find(accessToken, id);
    return this.toResponse(user);
  }

  private async find(accessToken: string, id: string): Promise<KeycloakUser> {
    try {
      return await this.keycloak.getUser(accessToken, id);
    } catch (error) {
      if (
        error instanceof KeycloakDependencyError &&
        error.upstreamStatus === 404
      ) {
        throw new NotFoundException();
      }
      throw error;
    }
  }

  private toResponse(user: KeycloakUser): UserResponse {
    return {
      id: user.id,
      username: user.username,
      "first-name": user.firstName ?? "",
      "last-name": user.lastName ?? "",
      enabled: user.enabled,
    };
  }
}

import { Injectable, NotFoundException } from "@nestjs/common";
import { KeycloakDependencyError } from "../common/errors";
import { CreateRoleDto } from "./dto/create-role.dto";
import { PatchRoleDto } from "./dto/patch-role.dto";
import { UpdateRoleDto } from "./dto/update-role.dto";
import { KeycloakAdminClient, KeycloakRole } from "./keycloak-admin.client";
import { RoleResponse } from "./interfaces/role-response.interface";

/**
 * Realm roles have no native soft-delete, so logical deletion is modelled with
 * a role attribute (`deleted=["true"]`): the role is kept in Keycloak but hidden
 * from every read path of this API.
 */
const DELETED_ATTRIBUTE = "deleted";

@Injectable()
export class RolesService {
  constructor(private readonly keycloak: KeycloakAdminClient) {}

  async create(dto: CreateRoleDto): Promise<RoleResponse> {
    await this.keycloak.createRole({
      name: dto.name,
      description: dto.description,
    });
    // Keycloak returns 201 with no body; re-read by name to expose the id.
    return this.toResponse(await this.keycloak.getRoleByName(dto.name));
  }

  async findAll(): Promise<RoleResponse[]> {
    const roles = await this.keycloak.listRoles();
    return roles
      .filter((role) => !this.isDeleted(role))
      .map((role) => this.toResponse(role));
  }

  async findOne(id: string): Promise<RoleResponse> {
    return this.toResponse(await this.getActiveRole(id));
  }

  async update(id: string, dto: UpdateRoleDto): Promise<RoleResponse> {
    const role = await this.getActiveRole(id);
    await this.keycloak.updateRoleById(id, {
      ...role,
      name: dto.name,
      description: dto.description,
    });
    return this.toResponse(await this.keycloak.getRoleById(id));
  }

  async patch(id: string, dto: PatchRoleDto): Promise<RoleResponse> {
    const role = await this.getActiveRole(id);
    await this.keycloak.updateRoleById(id, {
      ...role,
      name: dto.name ?? role.name,
      description: dto.description ?? role.description,
    });
    return this.toResponse(await this.keycloak.getRoleById(id));
  }

  async delete(id: string): Promise<void> {
    const role = await this.getActiveRole(id);
    await this.keycloak.updateRoleById(id, {
      ...role,
      attributes: { ...(role.attributes ?? {}), [DELETED_ATTRIBUTE]: ["true"] },
    });
  }

  async assignToUser(id: string, userId: string): Promise<void> {
    const role = await this.getActiveRole(id);
    await this.keycloak.assignRealmRole(userId, {
      id: role.id,
      name: role.name,
    });
  }

  async removeFromUser(id: string, userId: string): Promise<void> {
    const role = await this.getActiveRole(id);
    await this.keycloak.removeRealmRole(userId, {
      id: role.id,
      name: role.name,
    });
  }

  /**
   * Fetches a role by id, treating both a missing role and a logically deleted
   * one as 404.
   */
  private async getActiveRole(id: string): Promise<KeycloakRole> {
    let role: KeycloakRole;
    try {
      role = await this.keycloak.getRoleById(id);
    } catch (error) {
      if (
        error instanceof KeycloakDependencyError &&
        error.upstreamStatus === 404
      ) {
        throw new NotFoundException();
      }
      throw error;
    }
    if (this.isDeleted(role)) throw new NotFoundException();
    return role;
  }

  private isDeleted(role: KeycloakRole): boolean {
    return role.attributes?.[DELETED_ATTRIBUTE]?.includes("true") ?? false;
  }

  private toResponse(role: KeycloakRole): RoleResponse {
    return { id: role.id, name: role.name, description: role.description };
  }
}

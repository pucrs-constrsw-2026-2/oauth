import { Injectable } from '@nestjs/common';
import { KeycloakClientService } from '../common/keycloak/keycloak-client.service';
import { CreateRoleDto } from './dto/create-role.dto';
import { PatchRoleDto } from './dto/patch-role.dto';
import { RoleResponseDto } from './dto/role-response.dto';
import { UpdateRoleDto } from './dto/update-role.dto';

interface KeycloakRoleRepresentation {
  id?: string;
  name?: string;
  description?: string;
  composite?: boolean;
  clientRole?: boolean;
  containerId?: string;
}

function toRoleResponse(kc: KeycloakRoleRepresentation): RoleResponseDto {
  return { id: kc.id ?? '', name: kc.name ?? '', description: kc.description };
}

@Injectable()
export class RolesService {
  constructor(private readonly keycloak: KeycloakClientService) {}

  async create(token: string, dto: CreateRoleDto): Promise<RoleResponseDto> {
    await this.keycloak.adminRequest('POST', '/roles', token, {
      data: { name: dto.name, description: dto.description },
    });
    // Keycloak cria roles por nome; buscamos o id gerado em seguida.
    const created = await this.findByName(token, dto.name);
    return created;
  }

  async findAll(token: string): Promise<RoleResponseDto[]> {
    const { data } = await this.keycloak.adminRequest<
      KeycloakRoleRepresentation[]
    >('GET', '/roles', token);
    return data.map(toRoleResponse);
  }

  async findOne(token: string, id: string): Promise<RoleResponseDto> {
    const { data } = await this.keycloak.adminRequest<KeycloakRoleRepresentation>(
      'GET',
      `/roles-by-id/${id}`,
      token,
    );
    return toRoleResponse(data);
  }

  async replace(
    token: string,
    id: string,
    dto: UpdateRoleDto,
  ): Promise<void> {
    const current = await this.getRaw(token, id);
    await this.keycloak.adminRequest('PUT', `/roles-by-id/${id}`, token, {
      data: { ...current, name: dto.name, description: dto.description },
    });
  }

  async patch(token: string, id: string, dto: PatchRoleDto): Promise<void> {
    const current = await this.getRaw(token, id);
    await this.keycloak.adminRequest('PUT', `/roles-by-id/${id}`, token, {
      data: {
        ...current,
        name: dto.name ?? current.name,
        description: dto.description ?? current.description,
      },
    });
  }

  /**
   * Exclusao "logica" de um role. O Keycloak nao possui um flag de
   * habilitado/desabilitado para roles (diferente de usuarios); por isso,
   * aqui a exclusao remove de fato o role do realm - decisao documentada no
   * README.md.
   */
  async remove(token: string, id: string): Promise<void> {
    await this.keycloak.adminRequest('DELETE', `/roles-by-id/${id}`, token);
  }

  /** usado pelo modulo de assign/unassign de role em um user */
  async getRaw(
    token: string,
    id: string,
  ): Promise<KeycloakRoleRepresentation> {
    const { data } = await this.keycloak.adminRequest<KeycloakRoleRepresentation>(
      'GET',
      `/roles-by-id/${id}`,
      token,
    );
    return data;
  }

  private async findByName(
    token: string,
    name: string,
  ): Promise<RoleResponseDto> {
    const { data } = await this.keycloak.adminRequest<KeycloakRoleRepresentation>(
      'GET',
      `/roles/${encodeURIComponent(name)}`,
      token,
    );
    return toRoleResponse(data);
  }

  async assignToUser(token: string, userId: string, roleId: string): Promise<void> {
    const role = await this.getRaw(token, roleId);
    await this.keycloak.adminRequest(
      'POST',
      `/users/${userId}/role-mappings/realm`,
      token,
      { data: [{ id: role.id, name: role.name }] },
    );
  }

  async removeFromUser(
    token: string,
    userId: string,
    roleId: string,
  ): Promise<void> {
    const role = await this.getRaw(token, roleId);
    await this.keycloak.adminRequest(
      'DELETE',
      `/users/${userId}/role-mappings/realm`,
      token,
      { data: [{ id: role.id, name: role.name }] },
    );
  }
}

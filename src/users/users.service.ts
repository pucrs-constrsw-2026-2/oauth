import { Injectable } from '@nestjs/common';
import { KeycloakClientService } from '../common/keycloak/keycloak-client.service';
import { CreateUserDto } from './dto/create-user.dto';
import { ListUsersQueryDto } from './dto/list-users-query.dto';
import { UpdatePasswordDto } from './dto/update-password.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { UserResponseDto } from './dto/user-response.dto';

interface KeycloakUserRepresentation {
  id?: string;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
}

function toUserResponse(kc: KeycloakUserRepresentation): UserResponseDto {
  return {
    id: kc.id ?? '',
    username: kc.username ?? '',
    'first-name': kc.firstName ?? '',
    'last-name': kc.lastName ?? '',
    enabled: kc.enabled ?? false,
  };
}

@Injectable()
export class UsersService {
  constructor(private readonly keycloak: KeycloakClientService) {}

  async create(token: string, dto: CreateUserDto): Promise<UserResponseDto> {
    const { headers } = await this.keycloak.adminRequest(
      'POST',
      '/users',
      token,
      {
        data: {
          username: dto.username,
          email: dto.username,
          enabled: true,
          firstName: dto['first-name'],
          lastName: dto['last-name'],
          credentials: [
            { type: 'password', value: dto.password, temporary: false },
          ],
        },
      },
    );

    const location = headers['location'] ?? headers['Location'];
    const id = location ? location.split('/').pop()! : '';

    return {
      id,
      username: dto.username,
      'first-name': dto['first-name'],
      'last-name': dto['last-name'],
      enabled: true,
    };
  }

  async findAll(
    token: string,
    query: ListUsersQueryDto,
  ): Promise<UserResponseDto[]> {
    const { data } = await this.keycloak.adminRequest<
      KeycloakUserRepresentation[]
    >('GET', '/users', token, {
      params: query.enabled === undefined ? {} : { enabled: query.enabled },
    });
    return data.map(toUserResponse);
  }

  async findOne(token: string, id: string): Promise<UserResponseDto> {
    const { data } = await this.keycloak.adminRequest<KeycloakUserRepresentation>(
      'GET',
      `/users/${id}`,
      token,
    );
    return toUserResponse(data);
  }

  async update(token: string, id: string, dto: UpdateUserDto): Promise<void> {
    const payload: KeycloakUserRepresentation = {};
    if (dto.username !== undefined) {
      payload.username = dto.username;
      payload.email = dto.username;
    }
    if (dto['first-name'] !== undefined) payload.firstName = dto['first-name'];
    if (dto['last-name'] !== undefined) payload.lastName = dto['last-name'];
    if (dto.enabled !== undefined) payload.enabled = dto.enabled;

    await this.keycloak.adminRequest('PUT', `/users/${id}`, token, {
      data: payload,
    });
  }

  async updatePassword(
    token: string,
    id: string,
    dto: UpdatePasswordDto,
  ): Promise<void> {
    await this.keycloak.adminRequest('PUT', `/users/${id}/reset-password`, token, {
      data: { type: 'password', value: dto.password, temporary: false },
    });
  }

  /** exclusao logica: desabilita o usuario (nao remove do Keycloak) */
  async disable(token: string, id: string): Promise<void> {
    await this.keycloak.adminRequest('PUT', `/users/${id}`, token, {
      data: { enabled: false },
    });
  }
}

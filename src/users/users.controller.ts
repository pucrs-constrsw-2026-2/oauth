import {
  Body,
  Controller,
  Delete,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  Patch,
  Post,
  Put,
  Query,
  UseGuards,
} from '@nestjs/common';

import { AdministratorRoleGuard, BearerAuthGuard } from '../common';
import { KeycloakUsersService } from './keycloak-users.service';
import {
  ChangePasswordBody,
  CreateUserBody,
  UPDATABLE_USER_FIELDS,
  UpdateUserBody,
  UserRepresentation,
  toRepresentation,
} from './user.types';
import { userBadRequest } from './users.exceptions';

/**
 * Practical RFC 5322 address form: a dot-separated local part of permitted
 * atoms, then a domain with at least one dot. Quoted local parts and IP-literal
 * domains are rejected — they are valid in the RFC but are not usable as the
 * institutional e-mail this realm uses as the username.
 */
const RFC_5322_EMAIL =
  /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/;

const UUID =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@UseGuards(BearerAuthGuard, AdministratorRoleGuard)
@Controller('users')
export class UsersController {
  constructor(private readonly users: KeycloakUsersService) {}

  @Post()
  @HttpCode(HttpStatus.CREATED)
  async create(@Body() body: CreateUserBody): Promise<UserRepresentation> {
    const username = requireString(body?.username, 'username');
    const password = requireString(body?.password, 'password');

    if (!RFC_5322_EMAIL.test(username)) {
      throw userBadRequest('"username" must be a valid e-mail address.');
    }

    const created = await this.users.createUser(
      {
        username,
        // The realm uses the e-mail as the username, so both carry it.
        email: username,
        firstName: optionalString(body?.['first-name'], 'first-name'),
        lastName: optionalString(body?.['last-name'], 'last-name'),
      },
      password,
    );

    return toRepresentation(created);
  }

  @Get()
  async list(@Query('enabled') enabled?: string): Promise<UserRepresentation[]> {
    const users = await this.users.listUsers(parseEnabled(enabled));
    return users.map(toRepresentation);
  }

  @Get(':id')
  async get(@Param('id') id: string): Promise<UserRepresentation> {
    return toRepresentation(await this.users.getUser(requireId(id)));
  }

  @Put(':id')
  @HttpCode(HttpStatus.OK)
  async update(
    @Param('id') id: string,
    @Body() body: UpdateUserBody,
  ): Promise<void> {
    const changes = readUpdate(body);
    await this.users.updateUser(requireId(id), changes);
  }

  @Patch(':id')
  @HttpCode(HttpStatus.OK)
  async changePassword(
    @Param('id') id: string,
    @Body() body: ChangePasswordBody,
  ): Promise<void> {
    const password = requireString(body?.password, 'password');
    await this.users.changePassword(requireId(id), password);
  }

  @Delete(':id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async remove(@Param('id') id: string): Promise<void> {
    await this.users.disableUser(requireId(id));
  }
}

function requireString(value: unknown, field: string): string {
  if (typeof value !== 'string' || value.trim().length === 0) {
    throw userBadRequest(`"${field}" is required and must be a non-empty string.`);
  }
  return value.trim();
}

function optionalString(value: unknown, field: string): string | undefined {
  if (value === undefined || value === null) {
    return undefined;
  }
  if (typeof value !== 'string') {
    throw userBadRequest(`"${field}" must be a string.`);
  }
  return value.trim();
}

function requireId(id: string): string {
  if (!UUID.test(id)) {
    throw userBadRequest('The user id must be a UUID.');
  }
  return id;
}

function parseEnabled(value?: string): boolean | undefined {
  // No query string means the brief's default: only enabled users.
  if (value === undefined) {
    return true;
  }
  if (value === 'true') {
    return true;
  }
  if (value === 'false') {
    return false;
  }
  throw userBadRequest('"enabled" must be either "true" or "false".');
}

function readUpdate(body: UpdateUserBody): {
  firstName?: string;
  lastName?: string;
  enabled?: boolean;
} {
  if (typeof body !== 'object' || body === null) {
    throw userBadRequest('A JSON body is required.');
  }

  const unknownFields = Object.keys(body).filter(
    (key) => !UPDATABLE_USER_FIELDS.includes(key as never),
  );
  if (unknownFields.length > 0) {
    // Naming username explicitly: the realm uses e-mail as the username, so
    // changing it here would silently change the account's identity.
    const detail = unknownFields.includes('username')
      ? ' "username" is the account identity and cannot be changed on this route.'
      : '';
    throw userBadRequest(
      `Unsupported field(s): ${unknownFields.join(', ')}.${detail}`,
    );
  }

  const enabled = body.enabled;
  if (enabled !== undefined && typeof enabled !== 'boolean') {
    throw userBadRequest('"enabled" must be a boolean.');
  }

  const changes = {
    firstName: optionalString(body['first-name'], 'first-name'),
    lastName: optionalString(body['last-name'], 'last-name'),
    enabled,
  };

  if (
    changes.firstName === undefined &&
    changes.lastName === undefined &&
    changes.enabled === undefined
  ) {
    throw userBadRequest(
      `Provide at least one of: ${UPDATABLE_USER_FIELDS.join(', ')}.`,
    );
  }

  return changes;
}

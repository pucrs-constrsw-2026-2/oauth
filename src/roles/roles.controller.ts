import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Patch,
  Post,
  Put,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';

import { AdministratorRoleGuard, BearerAuthGuard } from '../common';
import { KeycloakAdminService } from './keycloak-admin.service';
import {
  roleBadRequest,
  roleNotFound,
} from './roles.exceptions';
import { RoleAssignmentInput, RoleInput } from './role.types';

@Controller()
@UseGuards(BearerAuthGuard, AdministratorRoleGuard)
export class RolesController {
  constructor(private readonly admin: KeycloakAdminService) {}

  @Post('roles')
  async create(@Body() body: RoleInput) {
    const role = validateRoleBody(body, true);
    return this.admin.createRole(role);
  }

  @Get('roles')
  list() {
    return this.admin.listRoles();
  }

  @Get('roles/:id')
  get(@Param('id') id: string) {
    validateId(id);
    return this.admin.getRole(id);
  }

  @Put('roles/:id')
  @HttpCode(HttpStatus.OK)
  async replace(@Param('id') id: string, @Body() body: RoleInput) {
    validateId(id);
    const role = validateRoleBody(body, true);
    await this.admin.replaceRole(id, role);
  }

  @Patch('roles/:id')
  @HttpCode(HttpStatus.OK)
  async update(@Param('id') id: string, @Body() body: RoleInput) {
    validateId(id);
    const role = validateRoleBody(body, false);
    if (Object.keys(role).length === 0) {
      throw roleBadRequest('PATCH body must include at least one mutable field.');
    }
    await this.admin.updateRole(id, role);
  }

  @Delete('roles/:id')
  @HttpCode(HttpStatus.NO_CONTENT)
  async remove(@Param('id') id: string) {
    validateId(id);
    await this.admin.logicalDeleteRole(id);
  }

  @Post('users/:id/roles')
  @HttpCode(HttpStatus.NO_CONTENT)
  async assign(@Param('id') userId: string, @Body() body: RoleAssignmentInput) {
    validateId(userId);
    const roleId = roleIdentifier(body);
    await this.admin.assignRole(userId, roleId);
  }

  @Delete('users/:id/roles/:roleId')
  @HttpCode(HttpStatus.NO_CONTENT)
  async unassign(@Param('id') userId: string, @Param('roleId') roleId: string) {
    validateId(userId);
    validateId(roleId);
    await this.admin.unassignRole(userId, roleId);
  }
}

function validateRoleBody(body: RoleInput, requireName: boolean): Record<string, unknown> {
  if (!body || typeof body !== 'object') {
    throw roleBadRequest('Role body must be a JSON object.');
  }
  const allowed = ['name', 'description', 'attributes'];
  if (Object.keys(body).some((key) => !allowed.includes(key))) {
    throw roleBadRequest('Role body contains unsupported fields.');
  }
  if (requireName && (typeof body.name !== 'string' || body.name.trim() === '')) {
    throw roleBadRequest('Role name is required.');
  }
  if (body.name !== undefined && typeof body.name !== 'string') {
    throw roleBadRequest('Role name must be a string.');
  }
  if (body.description !== undefined && typeof body.description !== 'string') {
    throw roleBadRequest('Role description must be a string.');
  }
  if (
    body.attributes !== undefined &&
    (!body.attributes || typeof body.attributes !== 'object' || Array.isArray(body.attributes))
  ) {
    throw roleBadRequest('Role attributes must be an object.');
  }
  return body as Record<string, unknown>;
}

function validateId(id: string): void {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id)) {
    throw roleBadRequest('Identifier must be a UUID.');
  }
}

function roleIdentifier(body: RoleAssignmentInput): string {
  const value = body?.roleId ?? body?.roleName ?? body?.name;
  if (typeof value !== 'string' || value.trim() === '') {
    throw roleBadRequest('A roleId or roleName is required.');
  }
  if (body.roleId !== undefined) {
    validateId(value);
  }
  return value;
}